package id.familyguard.dns

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import id.familyguard.FamilyGuardApp
import id.familyguard.ui.ThreatAdvisoryActivity
import java.util.concurrent.ConcurrentHashMap

object ThreatNotifier {

    private val lastAlertTime = ConcurrentHashMap<String, Long>()
    private const val DEBOUNCE_MILLIS = 60_000L // 1 notification per domain per 60s

    fun notifyThreatBlocked(context: Context, domain: String, category: String) {
        val now = System.currentTimeMillis()
        val lastTime = lastAlertTime[domain] ?: 0L

        if (now - lastTime < DEBOUNCE_MILLIS) {
            return // Suppress notification flooding
        }
        lastAlertTime[domain] = now

        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

        val advisoryIntent = Intent(context, ThreatAdvisoryActivity::class.java).apply {
            putExtra(ThreatAdvisoryActivity.EXTRA_TARGET_DOMAIN, domain)
            putExtra(ThreatAdvisoryActivity.EXTRA_THREAT_CATEGORY, category)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            domain.hashCode(),
            advisoryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FamilyGuardApp.CHANNEL_THREAT_ALERTS)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("🛡️ FamilyGuard: Ancaman Diblokir!")
            .setContentText("Akses ke situs mencurigakan '$domain' berhasil dicegah.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Akses ke situs mencurigakan '$domain' ($category) dicegah demi keselamatan keluarga. Sentuh untuk melihat detail keamanan.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(domain.hashCode(), notification)
    }

    internal fun clearThrottleHistory() {
        lastAlertTime.clear()
    }
}
