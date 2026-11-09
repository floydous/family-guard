package id.familyguard.data

import android.content.Context
import android.content.SharedPreferences
import id.familyguard.util.SecurityHelper

class AppPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    data class BlockedThreatRecord(
        val domain: String,
        val category: String,
        val timestamp: Long
    )

    var isDnsFilterEnabled: Boolean
        get() = prefs.getBoolean(KEY_DNS_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DNS_ENABLED, value).apply()

    var isWatchdogEnabled: Boolean
        get() = prefs.getBoolean(KEY_WATCHDOG_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WATCHDOG_ENABLED, value).apply()

    val isParentPinSet: Boolean
        get() = !prefs.getString(KEY_PIN_HASH, "").isNullOrEmpty()

    fun setParentPin(pin: String) {
        val hash = SecurityHelper.hashPin(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyParentPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, "") ?: return false
        return SecurityHelper.verifyPin(pin, storedHash)
    }

    var totalBlockedThreats: Int
        get() = prefs.getInt(KEY_TOTAL_BLOCKED, 0)
        private set(value) = prefs.edit().putInt(KEY_TOTAL_BLOCKED, value).apply()

    fun recordBlockedThreat(domain: String, category: String, timestamp: Long = System.currentTimeMillis()) {
        totalBlockedThreats += 1

        val currentLog = getRecentThreats().toMutableList()
        currentLog.add(0, BlockedThreatRecord(domain, category, timestamp))
        if (currentLog.size > MAX_LOG_SIZE) {
            currentLog.removeAt(currentLog.size - 1)
        }

        val serialized = serializeThreatList(currentLog)
        prefs.edit().putString(KEY_RECENT_THREATS, serialized).apply()
    }

    fun getRecentThreats(): List<BlockedThreatRecord> {
        val serialized = prefs.getString(KEY_RECENT_THREATS, "") ?: return emptyList()
        return deserializeThreatList(serialized)
    }

    fun clearThreatHistory() {
        prefs.edit().remove(KEY_RECENT_THREATS).putInt(KEY_TOTAL_BLOCKED, 0).apply()
    }

    companion object {
        private const val PREFS_NAME = "familyguard_preferences"
        private const val KEY_DNS_ENABLED = "pref_dns_enabled"
        private const val KEY_WATCHDOG_ENABLED = "pref_watchdog_enabled"
        private const val KEY_PIN_HASH = "pref_pin_hash"
        private const val KEY_TOTAL_BLOCKED = "pref_total_blocked"
        private const val KEY_RECENT_THREATS = "pref_recent_threats"
        private const val MAX_LOG_SIZE = 20

        internal fun serializeThreatList(list: List<BlockedThreatRecord>): String {
            return list.joinToString(";") { "${it.domain}|${it.category}|${it.timestamp}" }
        }

        internal fun deserializeThreatList(raw: String): List<BlockedThreatRecord> {
            if (raw.isBlank()) return emptyList()
            return raw.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size >= 3) {
                    BlockedThreatRecord(
                        domain = parts[0],
                        category = parts[1],
                        timestamp = parts[2].toLongOrNull() ?: 0L
                    )
                } else null
            }
        }
    }
}
