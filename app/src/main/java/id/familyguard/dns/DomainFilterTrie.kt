package id.familyguard.dns

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

class DomainFilterTrie {

    // High-performance map holding root wildcard rules (O(1) suffix search with category)
    private val blockedDomains: MutableMap<String, String> = ConcurrentHashMap()
    private val whitelist: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())

    data class FilterResult(
        val isBlocked: Boolean,
        val matchedRule: String? = null,
        val category: String? = null
    )

    val size: Int
        get() = blockedDomains.size

    /**
     * Inserts a single root domain rule with category (blocks the domain and all its subdomains).
     */
    fun insert(domain: String, category: String = "Iklan, Pelacak & Phising") {
        val cleanDomain = domain.trim().lowercase().removePrefix("www.").removePrefix("*.")
        if (cleanDomain.isNotEmpty() && cleanDomain.contains('.')) {
            blockedDomains[cleanDomain] = category
        }
    }

    /**
     * Loads hundreds of thousands of official blocklist rules from an asset stream (supports .gz).
     */
    fun loadFromStream(inputStream: InputStream) {
        val stream = if (isGzipStream(inputStream)) {
            GZIPInputStream(inputStream)
        } else {
            inputStream
        }

        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                    val clean = trimmed.removePrefix("0.0.0.0").removePrefix("127.0.0.1").trim().removePrefix("*.")
                    if (clean.contains('.')) {
                        blockedDomains[clean] = "Iklan, Pelacak & Phising"
                    }
                }
            }
        }
    }

    private fun isGzipStream(inputStream: InputStream): Boolean {
        // GZIP magic number: 0x1f, 0x8b
        return try {
            inputStream.mark(2)
            val byte1 = inputStream.read()
            val byte2 = inputStream.read()
            inputStream.reset()
            byte1 == 0x1F && byte2 == 0x8B
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a domain or any of its parent domains is present in the blocklist.
     * Evaluates suffixes from specific to root (e.g., a.b.ads.com -> b.ads.com -> ads.com).
     */
    fun match(domain: String): FilterResult {
        val cleanDomain = domain.trim().lowercase().removePrefix("www.")
        if (cleanDomain.isEmpty()) return FilterResult(false)

        // Check user whitelist first (Parent PIN override)
        if (whitelist.contains(cleanDomain)) {
            return FilterResult(false)
        }

        var candidate = cleanDomain
        while (true) {
            val category = blockedDomains[candidate]
            if (category != null) {
                return FilterResult(
                    isBlocked = true,
                    matchedRule = candidate,
                    category = category
                )
            }

            val dotIndex = candidate.indexOf('.')
            if (dotIndex == -1) break
            candidate = candidate.substring(dotIndex + 1)

            // Stop at top-level domain alone (don't check standalone ".com", ".net", ".id")
            if (!candidate.contains('.')) break
        }

        return FilterResult(false)
    }

    /**
     * Whitelists a domain temporarily or permanently (Parent Override).
     */
    fun addToWhitelist(domain: String) {
        whitelist.add(domain.trim().lowercase().removePrefix("www."))
    }

    fun removeFromWhitelist(domain: String) {
        whitelist.remove(domain.trim().lowercase().removePrefix("www."))
    }

    fun isWhitelisted(domain: String): Boolean {
        return whitelist.contains(domain.trim().lowercase().removePrefix("www."))
    }

    fun clear() {
        blockedDomains.clear()
        whitelist.clear()
    }
}
