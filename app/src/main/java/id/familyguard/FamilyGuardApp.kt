package id.familyguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FamilyGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Channel for VPN service persistent status
            val vpnChannel = NotificationChannel(
                CHANNEL_VPN_STATUS,
                "Status Perlindungan DNS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi status perlindungan DNS aktif di latar belakang"
            }

            // Channel for Threat Advisories (High Priority Heads-Up)
            val threatChannel = NotificationChannel(
                CHANNEL_THREAT_ALERTS,
                "Peringatan Ancaman Keamanan",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Peringatan langsung saat situs phising atau adware terdeteksi"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(vpnChannel)
            notificationManager.createNotificationChannel(threatChannel)
        }
    }

    companion object {
        const val CHANNEL_VPN_STATUS = "familyguard_vpn_status"
        const val CHANNEL_THREAT_ALERTS = "familyguard_threat_alerts"
    }
}
