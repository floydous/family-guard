package id.familyguard.watchdog

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri

class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_PACKAGE_ADDED) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        val replacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        // Only scan freshly installed apps, not updates to existing apps
        if (replacing) return

        // Scan package using DisguiseHeuristicsEngine
        val isSuspicious = DisguiseHeuristicsEngine.isSuspiciousPackage(context, packageName)

        if (isSuspicious) {
            triggerOneTapUninstall(context, packageName)
        }
    }

    private fun triggerOneTapUninstall(context: Context, packageName: String) {
        val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(uninstallIntent)
    }
}
