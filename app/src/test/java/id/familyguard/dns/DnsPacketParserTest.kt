package id.familyguard.dns

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

class DnsPacketParserTest {

    @Test
    fun testParseQuery_ValidAQuery_SuccessfullyParsed() {
        val rawQuery = buildSampleDnsQuery(
            transactionId = 0xABCD,
            domain = "google.com",
            qType = DnsPacketParser.TYPE_A
        )

        val query = DnsPacketParser.parseQuery(rawQuery)
        assertNotNull(query)
        assertEquals(0xABCD, query!!.transactionId)
        assertEquals("google.com", query.domain)
        assertEquals(DnsPacketParser.TYPE_A, query.qType)
        assertEquals(DnsPacketParser.CLASS_IN, query.qClass)
    }

    @Test
    fun testParseQuery_SubdomainQuery_SuccessfullyParsed() {
        val rawQuery = buildSampleDnsQuery(
            transactionId = 0x1234,
            domain = "login.scam-bank.com",
            qType = DnsPacketParser.TYPE_A
        )

        val query = DnsPacketParser.parseQuery(rawQuery)
        assertNotNull(query)
        assertEquals("login.scam-bank.com", query!!.domain)
    }

    @Test
    fun testBuildSinkholeResponse_TypeA_ReturnsZeroIp() {
        val query = DnsPacketParser.DnsQuery(
            transactionId = 0x4321,
            domain = "scam.com",
            qType = DnsPacketParser.TYPE_A,
            qClass = DnsPacketParser.CLASS_IN
        )

        val response = DnsPacketParser.buildSinkholeResponse(query, ttlSeconds = 60)
        assertNotNull(response)
        assertTrue(response.size > 12)

        val buffer = ByteBuffer.wrap(response)
        val txId = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF
        val qdCount = buffer.short.toInt() and 0xFFFF
        val anCount = buffer.short.toInt() and 0xFFFF

        assertEquals(0x4321, txId)
        // Check QR bit is 1 (Response)
        assertTrue((flags and 0x8000) != 0)
        assertEquals(1, qdCount)
        assertEquals(1, anCount)

        // The last 4 bytes of IPv4 answer must be 0.0.0.0
        val last4Bytes = response.takeLast(4).toByteArray()
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), last4Bytes)
    }

    @Test
    fun testBuildSinkholeResponse_TypeAAAA_ReturnsZeroIpv6() {
        val query = DnsPacketParser.DnsQuery(
            transactionId = 0x5555,
            domain = "tracker.net",
            qType = DnsPacketParser.TYPE_AAAA,
            qClass = DnsPacketParser.CLASS_IN
        )

        val response = DnsPacketParser.buildSinkholeResponse(query, ttlSeconds = 300)
        val last16Bytes = response.takeLast(16).toByteArray()
        assertArrayEquals(ByteArray(16), last16Bytes)
    }

    private fun buildSampleDnsQuery(transactionId: Int, domain: String, qType: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        data.writeShort(transactionId)
        data.writeShort(0x0100) // Standard query, RD=1
        data.writeShort(1) // QDCOUNT
        data.writeShort(0)
        data.writeShort(0)
        data.writeShort(0)

        for (part in domain.split('.')) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            data.writeByte(bytes.size)
            data.write(bytes)
        }
        data.writeByte(0)

        data.writeShort(qType)
        data.writeShort(DnsPacketParser.CLASS_IN)

        data.flush()
        return out.toByteArray()
    }
}
