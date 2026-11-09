package id.familyguard.watchdog

import kotlin.math.min

object LevenshteinDistance {

    /**
     * Calculates the Levenshtein distance between two strings using an O(min(m,n)) space complexity.
     */
    fun computeDistance(s1: String, s2: String): Int {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()

        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var prevRow = IntArray(b.length + 1) { it }
        var currRow = IntArray(b.length + 1)

        for (i in 1..a.length) {
            currRow[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                currRow[j] = min(
                    min(currRow[j - 1] + 1, prevRow[j] + 1),
                    prevRow[j - 1] + cost
                )
            }
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[b.length]
    }

    /**
     * Computes normalized similarity between 0.0 (completely different) and 1.0 (exact match).
     */
    fun computeSimilarity(s1: String, s2: String): Double {
        val a = s1.trim().lowercase()
        val b = s2.trim().lowercase()
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 1.0

        val distance = computeDistance(a, b)
        return (maxLen - distance).toDouble() / maxLen
    }

    /**
     * Returns true if s1 is a suspicious typosquat / impersonation of targetBrand.
     * Threshold: similarity >= 0.75 and distance > 0.
     */
    fun isTypoSquat(candidate: String, targetBrand: String, minSimilarity: Double = 0.75): Boolean {
        val a = candidate.trim().lowercase()
        val b = targetBrand.trim().lowercase()

        if (a == b) return false // Exact match is handled by package name verification
        val sim = computeSimilarity(a, b)
        return sim >= minSimilarity
    }
}
