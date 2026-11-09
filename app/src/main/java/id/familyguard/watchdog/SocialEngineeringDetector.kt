package id.familyguard.watchdog

object SocialEngineeringDetector {

    // Common social engineering lures frequently used in WhatsApp/Telegram APK malware
    private val SOCIAL_LURE_KEYWORDS = listOf(
        "undangan", "nikah", "pernikahan",
        "tilang", "etle",
        "paket", "resi", "kurir", "ekspedisi",
        "tagihan", "pln", "listrik",
        "pajak", "djp",
        "dokumen", "pdf",
        "invitation", "delivery", "receipt", "invoice"
    )

    private val SMS_STEALER_PERMISSIONS = setOf(
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS"
    )

    private const val ACCESSIBILITY_SERVICE_PERMISSION = "android.permission.BIND_ACCESSIBILITY_SERVICE"
    private const val NOTIFICATION_LISTENER_PERMISSION = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"

    data class ThreatEvaluation(
        val isThreat: Boolean,
        val riskScore: Int,
        val threatCategory: String,
        val details: String
    )

    /**
     * Evaluates whether an APK's identifier, label, and requested permissions match
     * the technical signature of social engineering OTP-stealers or accessibility trojans.
     */
    fun evaluate(
        fileName: String,
        appLabel: String,
        packageName: String,
        requestedPermissions: List<String>
    ): ThreatEvaluation {
        val normalizedText = "${fileName.lowercase()} ${appLabel.lowercase()}"
        val hasLureKeyword = matchesSocialLure(normalizedText)

        val smsPermissionsCount = requestedPermissions.count { SMS_STEALER_PERMISSIONS.contains(it) }
        val hasAccessibility = requestedPermissions.contains(ACCESSIBILITY_SERVICE_PERMISSION)
        val hasNotificationListener = requestedPermissions.contains(NOTIFICATION_LISTENER_PERMISSION)

        // Pattern 1: Document/Invitation lure combined with SMS-stealer permissions (Critical OTP Stealer)
        if (hasLureKeyword && smsPermissionsCount >= 1) {
            return ThreatEvaluation(
                isThreat = true,
                riskScore = 95,
                threatCategory = "Malware Pencuri OTP (SMS Stealer)",
                details = "Aplikasi menyamar sebagai dokumen atau undangan dan meminta izin membaca SMS perbankan/OTP."
            )
        }

        // Pattern 2: Document/Invitation lure combined with Accessibility service hijacking
        if (hasLureKeyword && hasAccessibility) {
            return ThreatEvaluation(
                isThreat = true,
                riskScore = 98,
                threatCategory = "Trojan Pembajak Layar (Accessibility Hijack)",
                details = "Aplikasi menyamar sebagai dokumen dan meminta kontrol penuh atas layar dan ketukan perangkat."
            )
        }

        // Pattern 3: Full SMS control (Receive + Send or Receive + Read) on any sideloaded application
        if (smsPermissionsCount >= 2) {
            return ThreatEvaluation(
                isThreat = true,
                riskScore = 80,
                threatCategory = "Aplikasi Berisiko Tinggi (Akses SMS)",
                details = "Aplikasi meminta izin penuh untuk membaca dan mengirimkan SMS tanpa verifikasi Google Play."
            )
        }

        // Pattern 4: Accessibility or Notification eavesdropping combined with social lure
        if (hasLureKeyword && hasNotificationListener) {
            return ThreatEvaluation(
                isThreat = true,
                riskScore = 85,
                threatCategory = "Spyware Penyadapan Notifikasi",
                details = "Aplikasi menyamar sebagai dokumen dan meminta izin menyadap seluruh notifikasi sistem."
            )
        }

        return ThreatEvaluation(
            isThreat = false,
            riskScore = 0,
            threatCategory = "Aman",
            details = "Tidak ditemukan pola ancaman rekayasa sosial atau pencurian OTP."
        )
    }

    internal fun matchesSocialLure(text: String): Boolean {
        val lower = text.lowercase()
        return SOCIAL_LURE_KEYWORDS.any { keyword -> lower.contains(keyword) }
    }
}
