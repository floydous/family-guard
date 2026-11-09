package id.familyguard.dns

import java.nio.ByteBuffer
import java.util.Collections
import java.util.LinkedHashMap

class DnsLruCache(
    private val maxCapacity: Int = DEFAULT_MAX_CAPACITY
) {

    data class CacheKey(val domain: String, val qType: Int)

    private class CacheEntry(
        val responsePayload: ByteArray,
        val expiresAtMillis: Long
    )

    // Thread-safe LinkedHashMap with LRU eviction ordering (accessOrder = true)
    private val lruMap: MutableMap<CacheKey, CacheEntry> = Collections.synchronizedMap(
        object : LinkedHashMap<CacheKey, CacheEntry>(maxCapacity, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<CacheKey, CacheEntry>?): Boolean {
                return size > maxCapacity
            }
        }
    )

    val size: Int
        get() = lruMap.size

    /**
     * Retrieves cached DNS response if present and not expired.
     * Rewrites the Transaction ID to match the incoming query so client resolvers accept the packet.
     */
    fun get(domain: String, qType: Int, transactionId: Int): ByteArray? {
        val key = CacheKey(domain.trim().lowercase(), qType)
        val entry = lruMap[key] ?: return null

        val now = System.currentTimeMillis()
        if (now >= entry.expiresAtMillis) {
            lruMap.remove(key)
            return null
        }

        // Clone response and patch Transaction ID (first 2 bytes)
        val response = entry.responsePayload.clone()
        if (response.size >= 2) {
            response[0] = ((transactionId shr 8) and 0xFF).toByte()
            response[1] = (transactionId and 0xFF).toByte()
        }
        return response
    }

    /**
     * Stores a resolved DNS response in the cache with dynamic TTL.
     */
    fun put(domain: String, qType: Int, responsePayload: ByteArray, ttlSecondsOverride: Long? = null) {
        if (responsePayload.size < 12) return

        val ttl = ttlSecondsOverride ?: extractTtlFromResponse(responsePayload)
        // Clamp TTL between 30 seconds and 3600 seconds (1 hour)
        val clampedTtl = ttl.coerceIn(MIN_TTL_SECONDS, MAX_TTL_SECONDS)
        val expiresAt = System.currentTimeMillis() + (clampedTtl * 1000L)

        val key = CacheKey(domain.trim().lowercase(), qType)
        lruMap[key] = CacheEntry(responsePayload.clone(), expiresAt)
    }

    fun clear() {
        lruMap.clear()
    }

    internal fun extractTtlFromResponse(response: ByteArray): Long {
        if (response.size < 12) return DEFAULT_TTL_SECONDS

        try {
            val buffer = ByteBuffer.wrap(response)
            buffer.position(4)
            val qdCount = buffer.short.toInt() and 0xFFFF
            val anCount = buffer.short.toInt() and 0xFFFF

            if (anCount == 0) return DEFAULT_TTL_SECONDS

            // Skip Header (12 bytes)
            buffer.position(12)

            // Skip Question sections
            for (i in 0 until qdCount) {
                while (buffer.hasRemaining()) {
                    val len = buffer.get().toInt() and 0xFF
                    if (len == 0) break
                    if ((len and 0xC0) == 0xC0) {
                        if (buffer.hasRemaining()) buffer.get()
                        break
                    }
                    if (buffer.remaining() >= len) {
                        buffer.position(buffer.position() + len)
                    } else return DEFAULT_TTL_SECONDS
                }
                if (buffer.remaining() >= 4) {
                    buffer.position(buffer.position() + 4) // Skip QTYPE and QCLASS
                }
            }

            // Parse first Answer section to read TTL
            if (buffer.hasRemaining()) {
                val b = buffer.get().toInt() and 0xFF
                if ((b and 0xC0) == 0xC0) {
                    if (buffer.hasRemaining()) buffer.get() // Skip pointer offset
                } else {
                    var len = b
                    while (len > 0 && buffer.hasRemaining()) {
                        buffer.position(buffer.position() + len)
                        len = buffer.get().toInt() and 0xFF
                    }
                }

                if (buffer.remaining() >= 8) {
                    buffer.position(buffer.position() + 4) // Skip TYPE (2) and CLASS (2)
                    val rawTtl = buffer.int.toLong() and 0xFFFFFFFFL
                    return rawTtl
                }
            }
        } catch (e: Exception) {
            // Malformed answer format; use safe default TTL
        }

        return DEFAULT_TTL_SECONDS
    }

    companion object {
        const val DEFAULT_MAX_CAPACITY = 2048
        const val MIN_TTL_SECONDS = 30L
        const val MAX_TTL_SECONDS = 3600L
        const val DEFAULT_TTL_SECONDS = 300L
    }
}
