package id.familyguard.dns

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

class DnsLruCacheTest {

    @Test
    fun testPutAndGet_RewritesTransactionId() {
        val cache = DnsLruCache(maxCapacity = 100)
        val sampleResponse = buildSampleDnsResponse(
            txId = 0x1111,
            domain = "google.com",
            ttl = 300
        )

        cache.put("google.com", DnsPacketParser.TYPE_A, sampleResponse, ttlSecondsOverride = 300)

        // Query cache with a new, different transaction ID: 0x9876
        val cachedResponse = cache.get("google.com", DnsPacketParser.TYPE_A, 0x9876)
        assertNotNull(cachedResponse)

        val buffer = ByteBuffer.wrap(cachedResponse!!)
        val rewrittenTxId = buffer.short.toInt() and 0xFFFF
        assertEquals(0x9876, rewrittenTxId)
    }

    @Test
    fun testCacheMiss_UncachedDomain_ReturnsNull() {
        val cache = DnsLruCache(maxCapacity = 100)
        val result = cache.get("uncached-domain.org", DnsPacketParser.TYPE_A, 0x1234)
        assertNull(result)
    }

    @Test
    fun testLruEviction_ExceedingCapacity_EvictsOldest() {
        val cache = DnsLruCache(maxCapacity = 2)
        val dummyPacket = ByteArray(16) { 0 }

        cache.put("domain1.com", 1, dummyPacket, 300)
        cache.put("domain2.com", 1, dummyPacket, 300)
        assertEquals(2, cache.size)

        // Adding a 3rd entry must evict domain1.com
        cache.put("domain3.com", 1, dummyPacket, 300)
        assertEquals(2, cache.size)

        assertNull(cache.get("domain1.com", 1, 0x1111))
        assertNotNull(cache.get("domain2.com", 1, 0x2222))
        assertNotNull(cache.get("domain3.com", 1, 0x3333))
    }

    @Test
    fun testExtractTtlFromResponse_ValidDnsPacket_ExtractsCorrectTtl() {
        val cache = DnsLruCache()
        val sampleResponse = buildSampleDnsResponse(
            txId = 0x5555,
            domain = "example.com",
            ttl = 720
        )

        val extractedTtl = cache.extractTtlFromResponse(sampleResponse)
        assertEquals(720L, extractedTtl)
    }

    private fun buildSampleDnsResponse(txId: Int, domain: String, ttl: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        data.writeShort(txId)
        data.writeShort(0x8180) // Standard response, NoError
        data.writeShort(1) // QDCOUNT
        data.writeShort(1) // ANCOUNT
        data.writeShort(0)
        data.writeShort(0)

        // Question
        for (part in domain.split('.')) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            data.writeByte(bytes.size)
            data.write(bytes)
        }
        data.writeByte(0)
        data.writeShort(DnsPacketParser.TYPE_A)
        data.writeShort(DnsPacketParser.CLASS_IN)

        // Answer
        data.writeShort(0xC00C) // Name pointer
        data.writeShort(DnsPacketParser.TYPE_A)
        data.writeShort(DnsPacketParser.CLASS_IN)
        data.writeInt(ttl) // 4-byte TTL
        data.writeShort(4) // RDLENGTH
        data.write(byteArrayOf(142.toByte(), 250.toByte(), 180.toByte(), 14)) // IP bytes

        data.flush()
        return out.toByteArray()
    }
}
