package id.familyguard.util

import org.junit.Assert.*
import org.junit.Test

class SecurityHelperTest {

    @Test
    fun testHashPin_ProducesConsistent64CharHex() {
        val hash1 = SecurityHelper.hashPin("1234")
        val hash2 = SecurityHelper.hashPin("1234")

        assertNotNull(hash1)
        assertEquals(64, hash1.length) // SHA-256 hex string is 64 characters
        assertEquals(hash1, hash2)
    }

    @Test
    fun testHashPin_DifferentPins_ProduceDifferentHashes() {
        val hash1 = SecurityHelper.hashPin("1234")
        val hash2 = SecurityHelper.hashPin("4321")

        assertNotEquals(hash1, hash2)
    }

    @Test
    fun testVerifyPin_CorrectPin_ReturnsTrue() {
        val storedHash = SecurityHelper.hashPin("9876")
        assertTrue(SecurityHelper.verifyPin("9876", storedHash))
    }

    @Test
    fun testVerifyPin_WrongPin_ReturnsFalse() {
        val storedHash = SecurityHelper.hashPin("9876")
        assertFalse(SecurityHelper.verifyPin("1111", storedHash))
    }

    @Test
    fun testVerifyPin_EmptyInputs_ReturnsFalse() {
        val storedHash = SecurityHelper.hashPin("9876")
        assertFalse(SecurityHelper.verifyPin("", storedHash))
        assertFalse(SecurityHelper.verifyPin("9876", ""))
    }
}
