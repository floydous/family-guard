package id.familyguard.data

import org.junit.Assert.*
import org.junit.Test

class AppPreferencesSerializationTest {

    @Test
    fun testSerialization_RoundtripPreservesData() {
        val originalList = listOf(
            AppPreferencesRepository.BlockedThreatRecord("scam-bank.com", "Phising", 1700000000000L),
            AppPreferencesRepository.BlockedThreatRecord("ad.tracker.net", "Iklan", 1700000050000L)
        )

        val serialized = AppPreferencesRepository.serializeThreatList(originalList)
        val deserialized = AppPreferencesRepository.deserializeThreatList(serialized)

        assertEquals(2, deserialized.size)
        assertEquals("scam-bank.com", deserialized[0].domain)
        assertEquals("Phising", deserialized[0].category)
        assertEquals(1700000000000L, deserialized[0].timestamp)

        assertEquals("ad.tracker.net", deserialized[1].domain)
        assertEquals("Iklan", deserialized[1].category)
        assertEquals(1700000050000L, deserialized[1].timestamp)
    }

    @Test
    fun testDeserialization_EmptyOrMalformedInput_HandlesGracefully() {
        assertTrue(AppPreferencesRepository.deserializeThreatList("").isEmpty())
        assertTrue(AppPreferencesRepository.deserializeThreatList("   ").isEmpty())
        assertTrue(AppPreferencesRepository.deserializeThreatList("malformed;corrupted").isEmpty())
    }
}
