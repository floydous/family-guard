package id.familyguard.dns

import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

class IpPacketHelperTest {

    @Test
    fun testExtractDnsPayload_ValidUdpDnsPacket_SuccessfullyExtracted() {
        val sampleDns = byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00)
        val packet = buildSyntheticIpUdpPacket(
            srcIp = byteArrayOf(10, 10, 10, 5),
            dstIp = byteArrayOf(10, 10, 10, 1),
            srcPort = 54321,
            dstPort = 53,
            payload = sampleDns
        )

        val context = IpPacketHelper.extractDnsPayload(packet, packet.size)
        assertNotNull(context)
        assertEquals(54321, context!!.clientPort)
        assertArrayEquals(byteArrayOf(10, 10, 10, 5), context.clientIp)
        assertArrayEquals(sampleDns, context.dnsPayload)
    }

    @Test
    fun testExtractDnsPayload_NonDnsPort_ReturnsNull() {
        val packet = buildSyntheticIpUdpPacket(
            srcIp = byteArrayOf(10, 10, 10, 5),
            dstIp = byteArrayOf(10, 10, 10, 1),
            srcPort = 54321,
            dstPort = 80, // HTTP port, not DNS
            payload = byteArrayOf(1, 2, 3)
        )

        val context = IpPacketHelper.extractDnsPayload(packet, packet.size)
        assertNull(context)
    }

    @Test
    fun testBuildUdpResponsePacket_ConstructsValidSwappedPacket() {
        val context = IpPacketHelper.DnsPacketContext(
            clientIp = byteArrayOf(192.toByte(), 168.toByte(), 1.toByte(), 100.toByte()),
            serverIp = byteArrayOf(10, 10, 10, 1),
            clientPort = 60000,
            dnsPayload = byteArrayOf(1, 2)
        )
        val dummyResponseDns = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte())

        val responsePacket = IpPacketHelper.buildUdpResponsePacket(context, dummyResponseDns)
        assertNotNull(responsePacket)
        assertEquals(20 + 8 + dummyResponseDns.size, responsePacket.size)

        val buffer = ByteBuffer.wrap(responsePacket)
        // IP header checks
        assertEquals(4, (buffer.get(0).toInt() and 0xFF) shr 4) // IPv4
        assertEquals(17, buffer.get(9).toInt() and 0xFF) // Protocol UDP

        // Check swapped IPs
        val srcIp = ByteArray(4)
        val dstIp = ByteArray(4)
        buffer.position(12)
        buffer.get(srcIp)
        buffer.get(dstIp)
        assertArrayEquals(context.serverIp, srcIp)
        assertArrayEquals(context.clientIp, dstIp)

        // UDP header checks
        buffer.position(20)
        assertEquals(53, buffer.short.toInt() and 0xFFFF) // Source is DNS (53)
        assertEquals(60000, buffer.short.toInt() and 0xFFFF) // Dest is client port
    }

    private fun buildSyntheticIpUdpPacket(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray
    ): ByteArray {
        val totalLength = 20 + 8 + payload.size
        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // IPv4 Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0.toByte())
        buffer.putShort(totalLength.toShort())
        buffer.putShort(1.toShort())
        buffer.putShort(0.toShort())
        buffer.put(64.toByte())
        buffer.put(17.toByte()) // UDP
        buffer.putShort(0.toShort()) // Checksum
        buffer.put(srcIp)
        buffer.put(dstIp)

        // UDP Header
        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putShort((8 + payload.size).toShort())
        buffer.putShort(0.toShort())

        // Payload
        buffer.put(payload)
        return packet
    }
}
