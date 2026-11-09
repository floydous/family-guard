package id.familyguard.dns

class DomainFilterTrie {

    private class Node {
        var isBlocked: Boolean = false
        var category: String? = null
        val children: MutableMap<String, Node> = HashMap()
    }

    private val root = Node()
    private val whitelist: MutableSet<String> = HashSet()

    data class FilterResult(
        val isBlocked: Boolean,
        val matchedRule: String? = null,
        val category: String? = null
    )

    /**
     * Inserts a domain rule into the reverse-trie (e.g., "ad.tracker.com" -> com -> tracker -> ad).
     */
    fun insert(domain: String, category: String = "Malware/Phising") {
        val cleanDomain = domain.trim().lowercase().removePrefix("www.")
        if (cleanDomain.isEmpty()) return

        val parts = cleanDomain.split('.').reversed()
        var current = root

        for (part in parts) {
            current = current.children.getOrPut(part) { Node() }
        }

        current.isBlocked = true
        current.category = category
    }

    /**
     * Checks if a domain (or any of its parent domains) is blocked.
     * Evaluates right-to-left: e.g. checking "a.b.scam.com" checks "scam.com" then "b.scam.com", etc.
     */
    fun match(domain: String): FilterResult {
        val cleanDomain = domain.trim().lowercase().removePrefix("www.")
        if (cleanDomain.isEmpty()) return FilterResult(false)

        // Check user whitelist first (Parent PIN override)
        if (whitelist.contains(cleanDomain)) {
            return FilterResult(false)
        }

        val parts = cleanDomain.split('.').reversed()
        var current = root
        var matchedDomainParts = mutableListOf<String>()

        for (part in parts) {
            val child = current.children[part] ?: break
            matchedDomainParts.add(part)
            current = child

            if (current.isBlocked) {
                val matchedRule = matchedDomainParts.reversed().joinToString(".")
                return FilterResult(
                    isBlocked = true,
                    matchedRule = matchedRule,
                    category = current.category ?: "Ancaman Keamanan"
                )
            }
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
        root.children.clear()
        whitelist.clear()
    }
}
