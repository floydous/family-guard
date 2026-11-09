package id.familyguard.util

import java.security.MessageDigest

object SecurityHelper {

    private const val DEFAULT_SALT = "familyguard_parent_pin_salt_v1"

    /**
     * Computes SHA-256 hash of a string combined with a salt.
     */
    fun hashPin(pin: String, salt: String = DEFAULT_SALT): String {
        val input = "$salt:$pin"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies if an input PIN matches the stored hash.
     */
    fun verifyPin(inputPin: String, storedHash: String, salt: String = DEFAULT_SALT): Boolean {
        if (inputPin.isEmpty() || storedHash.isEmpty()) return false
        val computedHash = hashPin(inputPin, salt)
        return computedHash.equals(storedHash, ignoreCase = true)
    }
}
