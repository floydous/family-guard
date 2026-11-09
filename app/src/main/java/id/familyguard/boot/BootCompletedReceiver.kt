package id.familyguard.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.core.content.ContextCompat
import id.familyguard.data.AppPreferencesRepository
import id.familyguard.dns.LocalDnsVpnService

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != ACTION_QUICKBOOT_POWERON &&
            action != ACTION_HTC_QUICKBOOT) {
            return
        }

        Log.i(TAG, "Device boot event received ($action), evaluating autostart state")

        val prefs = AppPreferencesRepository(context)

        // Only start if user previously enabled DNS protection
        if (prefs.isDnsFilterEnabled) {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent == null) {
                // VPN permission already granted by user
                val vpnIntent = Intent(context, LocalDnsVpnService::class.java).apply {
                    this.action = LocalDnsVpnService.ACTION_START
                }
                try {
                    ContextCompat.startForegroundService(context, vpnIntent)
                    Log.i(TAG, "Successfully triggered LocalDnsVpnService autostart on boot")
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start LocalDnsVpnService on boot", e)
                }
            } else {
                Log.w(TAG, "VPN permission consent is missing; cannot autostart headlessly")
            }
        }
    }

    companion object {
        private const val TAG = "BootCompletedReceiver"
        const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        const val ACTION_HTC_QUICKBOOT = "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}
