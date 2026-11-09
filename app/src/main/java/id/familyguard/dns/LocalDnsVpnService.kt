package id.familyguard.dns

import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import id.familyguard.FamilyGuardApp
import id.familyguard.ui.MainActivity
import java.io.IOException

class LocalDnsVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var isRunning = false

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

    private fun startVpn() {
        if (isRunning) return

        try {
            val builder = Builder()
                .setSession("FamilyGuard DNS Filter")
                .addAddress("10.10.10.1", 32)
                .addDnsServer("10.10.10.1")
                // Only route DNS traffic through VPN interface (Port 53)
                .addRoute("10.10.10.1", 32)
                .setBlocking(false)

            vpnInterface = builder.establish()
            isRunning = true

            // TODO: Start UDP packet handling coroutine loop on vpnInterface
        } catch (e: Exception) {
            stopVpn()
        }
    }

    private fun stopVpn() {
        isRunning = false
        try {
            vpnInterface?.close()
        } catch (ignored: IOException) {}
        vpnInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
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
    }
}
