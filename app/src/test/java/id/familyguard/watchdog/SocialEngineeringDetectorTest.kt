package id.familyguard.watchdog

import org.junit.Assert.*
import org.junit.Test

class SocialEngineeringDetectorTest {

    @Test
    fun testWeddingInvitationWithSmsStealer_IsCriticalThreat() {
        val result = SocialEngineeringDetector.evaluate(
            fileName = "Surat Undangan Pernikahan.apk",
            appLabel = "Undangan Digital",
            packageName = "com.wedding.invitation.app",
            requestedPermissions = listOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
                "android.permission.INTERNET"
            )
        )

        assertTrue(result.isThreat)
        assertEquals(95, result.riskScore)
        assertEquals("Malware Pencuri OTP (SMS Stealer)", result.threatCategory)
    }

    @Test
    fun testPoliceTicketWithSmsPermissions_IsCriticalThreat() {
        val result = SocialEngineeringDetector.evaluate(
            fileName = "Surat Tilang-1.0.apk",
            appLabel = "Surat Tilang Digital",
            packageName = "com.etle.polri.surat",
            requestedPermissions = listOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.SEND_SMS",
                "android.permission.INTERNET"
            )
        )

        assertTrue(result.isThreat)
        assertEquals(95, result.riskScore)
        assertEquals("Malware Pencuri OTP (SMS Stealer)", result.threatCategory)
    }

    @Test
    fun testPackageDeliveryWithAccessibility_IsCriticalThreat() {
        val result = SocialEngineeringDetector.evaluate(
            fileName = "Cek Resi Paket J&T.apk",
            appLabel = "Lacak Paket Kurir",
            packageName = "com.resi.paket.tracking",
            requestedPermissions = listOf(
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.INTERNET"
            )
        )

        assertTrue(result.isThreat)
        assertEquals(98, result.riskScore)
        assertEquals("Trojan Pembajak Layar (Accessibility Hijack)", result.threatCategory)
    }

    @Test
    fun testNotificationSpyware_IsThreat() {
        val result = SocialEngineeringDetector.evaluate(
            fileName = "Dokumen Tagihan PLN.apk",
            appLabel = "Tagihan Listrik",
            packageName = "com.tagihan.pln.dokumen",
            requestedPermissions = listOf(
                "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
                "android.permission.INTERNET"
            )
        )

        assertTrue(result.isThreat)
        assertEquals(85, result.riskScore)
        assertEquals("Spyware Penyadapan Notifikasi", result.threatCategory)
    }

    @Test
    fun testBenignApp_WithoutLureOrDangerousPermissions_IsNotThreat() {
        val result = SocialEngineeringDetector.evaluate(
            fileName = "Kalkulator Sederhana.apk",
            appLabel = "Kalkulator Cepat",
            packageName = "com.tools.calculator.simple",
            requestedPermissions = listOf(
                "android.permission.VIBRATE"
            )
        )

        assertFalse(result.isThreat)
        assertEquals(0, result.riskScore)
    }
}
