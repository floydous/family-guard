package id.familyguard.watchdog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevenshteinDistanceTest {

    @Test
    fun testDistance_ExactMatch_ReturnsZero() {
        assertEquals(0, LevenshteinDistance.computeDistance("Google", "google"))
        assertEquals(1.0, LevenshteinDistance.computeSimilarity("WhatsApp", "whatsapp"), 0.001)
    }

    @Test
    fun testDistance_OneInsertion_ReturnsOne() {
        assertEquals(1, LevenshteinDistance.computeDistance("Gooogle", "Google"))
    }

    @Test
    fun testDistance_OneDeletion_ReturnsOne() {
        assertEquals(1, LevenshteinDistance.computeDistance("WhatApp", "WhatsApp"))
    }

    @Test
    fun testSimilarity_TypoSquat_Detected() {
        // "Gooogle" (len 7, distance 1 -> similarity 6/7 = 0.857 >= 0.75)
        assertTrue(LevenshteinDistance.isTypoSquat("Gooogle Photos", "Google Photos"))
        assertTrue(LevenshteinDistance.isTypoSquat("WhatApp", "WhatsApp"))
    }

    @Test
    fun testSimilarity_UnrelatedString_NotTypoSquat() {
        assertFalse(LevenshteinDistance.isTypoSquat("Kalkulator", "Google Chrome"))
        assertFalse(LevenshteinDistance.isTypoSquat("Resep Makanan", "WhatsApp"))
    }

    @Test
    fun testApkQuarantineRiskEvaluation_ImpersonatingBrand_IsFlagged() {
        val isRisky = ApkQuarantineScanner.evaluateApkRisk(
            appLabel = "Google Photos",
            packageName = "com.untrusted.fake.photos",
            hasOverlay = true
        )
        assertTrue(isRisky)
    }

    @Test
    fun testApkQuarantineRiskEvaluation_LegitimateApp_NotFlagged() {
        val isRisky = ApkQuarantineScanner.evaluateApkRisk(
            appLabel = "Google Photos",
            packageName = "com.google.android.apps.photos",
            hasOverlay = false
        )
        assertFalse(isRisky)
    }
}
