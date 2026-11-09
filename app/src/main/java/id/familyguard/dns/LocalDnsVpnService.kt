package id.familyguard.dns

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import id.familyguard.FamilyGuardApp
import id.familyguard.ui.MainActivity
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class LocalDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
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

    private fun startVpn() {
        if (isRunning) return

        try {
            loadBlocklists()

            val builder = Builder()
                .setSession("FamilyGuard")
                .addAddress("10.254.1.2", 24)
                .addDnsServer("10.254.1.1")
                .addRoute("10.254.1.1", 32)

            // Intercept direct queries to public resolvers (prevents Chrome & apps from bypassing VPN)
            val publicDnsRoutes = listOf(
                "1.1.1.1", "1.0.0.1",         // Cloudflare
                "8.8.8.8", "8.8.4.4",         // Google
                "9.9.9.9", "149.112.112.112",   // Quad9
                "208.67.222.222", "208.67.220.220" // OpenDNS
            )
            for (dnsIp in publicDnsRoutes) {
                builder.addRoute(dnsIp, 32)
            }

            builder.setBlocking(true)

            vpnInterface = builder.establish()
            isRunning = true

            startPacketProcessingLoop()
        } catch (e: Exception) {
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
                        // 1. Synthesize 0.0.0.0 Sinkhole response
                        val sinkholePayload = DnsPacketParser.buildSinkholeResponse(query)
                        val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, sinkholePayload)

                        synchronized(outStream) {
                            try {
                                outStream.write(responsePacket)
                            } catch (ignored: IOException) {}
                        }

                        // 2. Trigger non-blocking Heads-Up Notification (throttled)
                        ThreatNotifier.notifyThreatBlocked(
                            this@LocalDnsVpnService,
                            query.domain,
                            filterResult.category ?: "Ancaman Keamanan"
                        )
                    } else {
                        // Forward legitimate queries to protected upstream DNS servers
                        launch {
                            val upstreamResponse = DnsUpstreamForwarder.forward(
                                this@LocalDnsVpnService,
                                dnsContext.dnsPayload
                            )
                            if (upstreamResponse != null) {
                                val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, upstreamResponse)
                                synchronized(outStream) {
                                    try {
                                        outStream.write(responsePacket)
                                    } catch (ignored: IOException) {}
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
    }
}
