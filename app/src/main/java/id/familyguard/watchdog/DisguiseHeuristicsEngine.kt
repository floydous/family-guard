package id.familyguard.watchdog

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object DisguiseHeuristicsEngine {

    // Known target brands frequently impersonated by adware
    private val OFFICIAL_APP_CATALOG = mapOf(
        "google photos" to setOf("com.google.android.apps.photos"),
        "photos" to setOf("com.google.android.apps.photos"),
        "whatsapp" to setOf("com.whatsapp", "com.whatsapp.w4b"),
        "google chrome" to setOf("com.android.chrome"),
        "chrome" to setOf("com.android.chrome"),
        "system update" to setOf("com.google.android.gms", "com.android.settings"),
        "pembersih ram" to emptySet(),
        "ram cleaner" to emptySet()
    )

    fun isSuspiciousPackage(context: Context, packageName: String): Boolean {
        val pm = context.packageManager

        try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appLabel = pm.getApplicationLabel(appInfo).toString().trim().lowercase()

            val hasHiddenLauncher = isLauncherHidden(pm, packageName)
            val hasOverlayPermission = hasPermission(context, packageName, "android.permission.SYSTEM_ALERT_WINDOW")
            val isImpersonating = checkImpersonation(appLabel, packageName)

            val riskScore = calculateRiskScore(isImpersonating, hasHiddenLauncher, hasOverlayPermission)
            return riskScore >= 60
        } catch (e: Exception) {
            return false
        }
    }

    internal fun calculateRiskScore(
        isImpersonating: Boolean,
        hasHiddenLauncher: Boolean,
        hasOverlayPermission: Boolean
    ): Int {
        var riskScore = 0
        if (isImpersonating) riskScore += 50
        if (hasHiddenLauncher) riskScore += 30
        if (hasOverlayPermission) riskScore += 30
        return riskScore
    }

    private fun isLauncherHidden(pm: PackageManager, packageName: String): Boolean {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setPackage(packageName)
        }
        val activities = pm.queryIntentActivities(launcherIntent, 0)
        return activities.isEmpty()
    }

    private fun hasPermission(context: Context, packageName: String, permission: String): Boolean {
        val pm = context.packageManager
        try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions ?: return false
            return requestedPermissions.contains(permission)
        } catch (e: Exception) {
            return false
        }
    }

    internal fun checkImpersonation(appLabel: String, packageName: String): Boolean {
        for ((targetBrand, validPackages) in OFFICIAL_APP_CATALOG) {
            if (appLabel == targetBrand || appLabel.contains(targetBrand)) {
                if (!validPackages.contains(packageName)) {
                    return true
                }
            }
        }
        return false
    }
}
