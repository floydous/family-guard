package id.familyguard.watchdog

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import id.familyguard.FamilyGuardApp
import java.io.File

object ApkQuarantineScanner {

    const val QUARANTINE_EXTENSION = ".quarantine"

    fun scanAndQuarantine(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists() || !apkFile.name.endsWith(".apk", ignoreCase = true)) {
            return false
        }

        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, PackageManager.GET_PERMISSIONS)
                ?: return false

            val appInfo = packageInfo.applicationInfo ?: return false
            appInfo.sourceDir = apkFile.absolutePath
            appInfo.publicSourceDir = apkFile.absolutePath

            val appLabel = (pm.getApplicationLabel(appInfo) ?: "").toString()
            val packageName = packageInfo.packageName ?: ""
            val requestedPermissions = packageInfo.requestedPermissions?.toList() ?: emptyList()
            val hasOverlay = requestedPermissions.contains("android.permission.SYSTEM_ALERT_WINDOW")

            val isThreat = evaluateApkRisk(apkFile.name, appLabel, packageName, requestedPermissions, hasOverlay)

            if (isThreat) {
                val quarantinedFile = File(apkFile.parentFile, apkFile.name + QUARANTINE_EXTENSION)
                val success = apkFile.renameTo(quarantinedFile)
                if (success) {
                    notifyQuarantineAction(context, apkFile.name, appLabel)
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore corrupted or unreadable APKs
        }
        return false
    }

    internal fun evaluateApkRisk(
        fileName: String = "",
        appLabel: String,
        packageName: String,
        requestedPermissions: List<String> = emptyList(),
        hasOverlay: Boolean = false
    ): Boolean {
        // 1. Social Engineering & OTP Stealer check (Undangan, Tilang, Paket lures with SMS/Accessibility permissions)
        val socialThreat = SocialEngineeringDetector.evaluate(fileName, appLabel, packageName, requestedPermissions)
        if (socialThreat.isThreat) return true

        // 2. Brand Impersonation check (Google Photos, WhatsApp fake clones)
        val isImpersonating = DisguiseHeuristicsEngine.checkImpersonation(appLabel, packageName)
        return isImpersonating
    }

    private fun notifyQuarantineAction(context: Context, fileName: String, appLabel: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return

        val notification = NotificationCompat.Builder(context, FamilyGuardApp.CHANNEL_THREAT_ALERTS)
            .setContentTitle("FamilyGuard: Berkas Dikarantina")
            .setContentText("Aplikasi penyamar '$appLabel' ($fileName) berhasil dinetralisir dari unduhan.")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(fileName.hashCode(), notification)
    }
}
