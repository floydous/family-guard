package id.familyguard.dns

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
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

    private val dohResolver = DohUpstreamResolver()

    override fun onCreate() {
        super.onCreate()
        seedDefaultRules()
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

    private fun seedDefaultRules() {
        // Seed default high-confidence threat rules (HaGeZi & OISD samples)
        filterTrie.insert("scam-bank-login.com", "Phising Perbankan")
        filterTrie.insert("hadiah-gratis.id", "Scam Penipuan")
        filterTrie.insert("update-system-secure.com", "Malware Adware")
        filterTrie.insert("doubleclick.net", "Pelacak & Iklan Berbahaya")
        filterTrie.insert("adservice.google.com", "Iklan Agresif")
    }

    private fun startVpn() {
        if (isRunning) return

        try {
            val builder = Builder()
                .setSession("FamilyGuard DNS Filter")
                .addAddress("10.10.10.1", 32)
                .addDnsServer("10.10.10.1")
                // Only intercept DNS Port 53
                .addRoute("10.10.10.1", 32)
                .setBlocking(true)

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
                            outStream.write(responsePacket)
                        }

                        // 2. Trigger non-blocking Heads-Up Notification (throttled)
                        ThreatNotifier.notifyThreatBlocked(
                            this@LocalDnsVpnService,
                            query.domain,
                            filterResult.category ?: "Ancaman Keamanan"
                        )
                    } else {
                        // Forward legitimate queries to encrypted DoH upstream
                        launch {
                            val dohResponse = dohResolver.resolve(dnsContext.dnsPayload)
                            if (dohResponse != null) {
                                val responsePacket = IpPacketHelper.buildUdpResponsePacket(dnsContext, dohResponse)
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
        const val ACTION_START = "id.familyguard.action.START_VPN"
        const val ACTION_STOP = "id.familyguard.action.STOP_VPN"
        private const val NOTIFICATION_ID = 1001

        val filterTrie = DomainFilterTrie()
    }
}
