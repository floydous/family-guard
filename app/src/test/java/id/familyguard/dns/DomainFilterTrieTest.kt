package id.familyguard.dns

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DomainFilterTrieTest {

    private lateinit var trie: DomainFilterTrie

    @Before
    fun setUp() {
        trie = DomainFilterTrie()
        trie.insert("doubleclick.net", "Pelacak Iklan")
        trie.insert("scam-bank.com", "Phising Perbankan")
        trie.insert("malware-drop.xyz", "Malware")
    }

    @Test
    fun testExactMatch_IsBlocked() {
        val result = trie.match("doubleclick.net")
        assertTrue(result.isBlocked)
        assertEquals("doubleclick.net", result.matchedRule)
        assertEquals("Pelacak Iklan", result.category)
    }

    @Test
    fun testSubdomainMatch_IsBlocked() {
        val r1 = trie.match("ad.doubleclick.net")
        assertTrue(r1.isBlocked)
        assertEquals("doubleclick.net", r1.matchedRule)

        val r2 = trie.match("cdn.ads.static.doubleclick.net")
        assertTrue(r2.isBlocked)
        assertEquals("doubleclick.net", r2.matchedRule)
    }

    @Test
    fun testUnrelatedDomain_IsNotBlocked() {
        val r1 = trie.match("google.com")
        assertFalse(r1.isBlocked)

        // Substring boundary check: notdoubleclick.net should NOT match doubleclick.net
        val r2 = trie.match("notdoubleclick.net")
        assertFalse(r2.isBlocked)

        val r3 = trie.match("bank.com")
        assertFalse(r3.isBlocked)
    }

    @Test
    fun testWwwPrefix_NormalizesAndBlocks() {
        val result = trie.match("www.scam-bank.com")
        assertTrue(result.isBlocked)
        assertEquals("scam-bank.com", result.matchedRule)
    }

    @Test
    fun testParentWhitelistOverride_UnblocksSpecificDomain() {
        assertTrue(trie.match("ad.doubleclick.net").isBlocked)

        // Whitelist ad.doubleclick.net
        trie.addToWhitelist("ad.doubleclick.net")

        // Specific whitelisted domain should now pass
        assertFalse(trie.match("ad.doubleclick.net").isBlocked)

        // Other subdomains or base domain remain blocked
        assertTrue(trie.match("other.doubleclick.net").isBlocked)
        assertTrue(trie.match("doubleclick.net").isBlocked)
    }
}
