package id.familyguard.watchdog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisguiseHeuristicsEngineTest {

    @Test
    fun testImpersonationDetection_FakeGooglePhotos_ReturnsTrue() {
        // App named "Google Photos" but with shady package name
        val isImpersonating = DisguiseHeuristicsEngine.checkImpersonation(
            appLabel = "Google Photos",
            packageName = "com.super.cleaner.photos.ads"
        )
        assertTrue(isImpersonating)
    }

    @Test
    fun testImpersonationDetection_OfficialGooglePhotos_ReturnsFalse() {
        // Official Google Photos package
        val isImpersonating = DisguiseHeuristicsEngine.checkImpersonation(
            appLabel = "Google Photos",
            packageName = "com.google.android.apps.photos"
        )
        assertFalse(isImpersonating)
    }

    @Test
    fun testImpersonationDetection_PembersihRam_AlwaysReturnsTrue() {
        // Known generic adware bait name
        val isImpersonating = DisguiseHeuristicsEngine.checkImpersonation(
            appLabel = "Pembersih RAM Cepat",
            packageName = "com.cleaner.booster.fast"
        )
        assertTrue(isImpersonating)
    }

    @Test
    fun testImpersonationDetection_UnrelatedApp_ReturnsFalse() {
        val isImpersonating = DisguiseHeuristicsEngine.checkImpersonation(
            appLabel = "Buku Resep Masakan",
            packageName = "com.resep.masakan.nusantara"
        )
        assertFalse(isImpersonating)
    }

    @Test
    fun testRiskScoreCalculation_ImpersonatingAndOverlay_IsSuspicious() {
        val score = DisguiseHeuristicsEngine.calculateRiskScore(
            isImpersonating = true,
            hasHiddenLauncher = false,
            hasOverlayPermission = true
        )
        // 50 + 30 = 80 >= 60 threshold
        assertEquals(80, score)
        assertTrue(score >= 60)
    }

    @Test
    fun testRiskScoreCalculation_OnlyOverlayPermission_IsNotSuspicious() {
        val score = DisguiseHeuristicsEngine.calculateRiskScore(
            isImpersonating = false,
            hasHiddenLauncher = false,
            hasOverlayPermission = true
        )
        // 30 < 60 threshold (legitimate chat heads app)
        assertEquals(30, score)
        assertFalse(score >= 60)
    }
}
