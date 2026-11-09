package id.familyguard.dns

import android.app.PendingIntent
import android.content.Intent
import android.net.*
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import id.familyguard.FamilyGuardApp
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetAddress

class LocalDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    @Volatile
    private var activeDnsServers: List<InetAddress> = emptyList()

    private lateinit var prefs: AppPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferencesRepository(this)
        serviceScope.launch(Dispatchers.IO) {
            loadBlocklists()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification())
        startVpn()
        return START_STICKY
    }

    private fun loadBlocklists() {
        if (filterTrie.size > 1000) return
        val assetNames = assets.list("") ?: emptyArray()
        val filename = if (assetNames.contains("blocklists.txt")) "blocklists.txt" else "blocklists"

        try {
            assets.open(filename).use { stream ->
                filterTrie.loadFromStream(stream)
            }
            Log.i(TAG, "Successfully loaded ${filterTrie.size} official blocklist rules from assets/$filename")
        } catch (e: IOException) {
            Log.e(TAG, "Critical error: unable to load blocklist asset '$filename'", e)
            throw IllegalStateException("Blocklist asset '$filename' is missing or unreadable", e)
        }
    }

    private fun discoverPhysicalDnsServers(): List<InetAddress> {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return emptyList()
        val network = cm.activeNetwork ?: return emptyList()
        val lp = cm.getLinkProperties(network) ?: return emptyList()
        return lp.dnsServers
    }

    private fun registerNetworkTracker() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
                activeDnsServers = discoverPhysicalDnsServers()
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                setUnderlyingNetworks(arrayOf(network))
                activeDnsServers = linkProperties.dnsServers
            }

            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
                activeDnsServers = emptyList()
            }
        }

        try {
            cm.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register network callback for link properties tracking", e)
        }
    }

    private fun unregisterNetworkTracker() {
        networkCallback?.let {
            val cm = getSystemService(ConnectivityManager::class.java)
            try {
                cm?.unregisterNetworkCallback(it)
            } catch (ignored: Exception) {}
        }
        networkCallback = null
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            loadBlocklists()
            registerNetworkTracker()

            val cm = getSystemService(ConnectivityManager::class.java)
            val currentNetwork = cm?.activeNetwork
            if (currentNetwork != null) {
                setUnderlyingNetworks(arrayOf(currentNetwork))
            }

            // Only divert DNS port 53 to our local tunnel IP 10.254.1.1
            // Never add routes for public DNS resolvers (1.1.1.1/8.8.8.8) so the forwarder can reach them over physical link
            val builder = Builder()
                .setSession("FamilyGuard")
                .addAddress("10.254.1.2", 24)
                .addDnsServer("10.254.1.1")
                .addRoute("10.254.1.1", 32)
                .setBlocking(true)

            vpnInterface = builder.establish()
            isRunning = true

            startPacketProcessingLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Fatal: unable to establish VPN interface", e)
            stopVpn()
        }
    }

    private fun startPacketProcessingLoop() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val inStream = FileInputStream(fd)
        val outStream = FileOutputStream(fd)

        serviceScope.launch {
            val buffer = ByteArray(4096)

            while (isActive && isRunning) {
                try {
                    val length = inStream.read(buffer)
                    if (length <= 0) continue

                    val dnsContext = IpPacketHelper.extractDnsPayload(buffer, length) ?: continue
                    val query = DnsPacketParser.parseQuery(dnsContext.dnsPayload) ?: continue

                    val filterResult = filterTrie.match(query.domain)

                    if (filterResult.isBlocked) {
                        // 1. Synthesize 0.0.0.0 Sinkhole response immediately
                        val sinkholePayload = DnsPacketParser.buildSinkholeResponse(query)
                        val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, sinkholePayload)

                        synchronized(outStream) {
                            try {
                                outStream.write(responsePacket)
                            } catch (ignored: IOException) {}
                        }

                        // 2. Record blocked domain silently to dashboard log (NO NOTIFICATION SPAM for background ad requests)
                        try {
                            prefs.recordBlockedThreat(query.domain, filterResult.category ?: "Iklan & Pelacak")
                        } catch (ignored: Exception) {}
                    } else {
                        // 1. Check in-memory LRU TTL Cache first (0.00ms instantaneous response)
                        val cachedResponse = dnsCache.get(query.domain, query.qType, query.transactionId)
                        if (cachedResponse != null) {
                            val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, cachedResponse)
                            synchronized(outStream) {
                                try {
                                    outStream.write(responsePacket)
                                } catch (ignored: IOException) {}
                            }
                        } else {
                            // 2. Cache miss: Forward query dynamically to physical/upstream DNS
                            launch {
                                val upstreamResponse = DnsUpstreamForwarder.forward(
                                    this@LocalDnsVpnService,
                                    dnsContext.dnsPayload,
                                    activeDnsServers
                                )
                                if (upstreamResponse != null) {
                                    // Cache response with extracted TTL for subsequent lookups
                                    dnsCache.put(query.domain, query.qType, upstreamResponse)

                                    val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, upstreamResponse)
                                    synchronized(outStream) {
                                        try {
                                            outStream.write(responsePacket)
                                        } catch (ignored: IOException) {}
                                    }
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    break
                } catch (e: Exception) {
                    // Continue reading next packet
                }
            }
        }
    }

    private fun stopVpn() {
        isRunning = false
        unregisterNetworkTracker()
        serviceJob.cancelChildren()

        try {
            vpnInterface?.close()
        } catch (ignored: IOException) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotification() = NotificationCompat.Builder(this, FamilyGuardApp.CHANNEL_VPN_STATUS)
        .setContentTitle("FamilyGuard Aktif")
        .setContentText("Melindungi keluarga dari phising dan adware")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .build()

    companion object {
        private const val TAG = "LocalDnsVpnService"
        const val ACTION_START = "id.familyguard.action.START_VPN"
        const val ACTION_STOP = "id.familyguard.action.STOP_VPN"
        private const val NOTIFICATION_ID = 1001

        val filterTrie = DomainFilterTrie()
        val dnsCache = DnsLruCache()
    }
}
