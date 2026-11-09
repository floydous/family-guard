package id.familyguard.dns

import java.nio.ByteBuffer

object IpPacketHelper {

    /**
     * Extracts the UDP payload from a raw IPv4 packet if it targets UDP port 53.
     * Returns null if not IPv4, not UDP, or not port 53.
     */
    fun extractDnsPayload(packet: ByteArray, length: Int): DnsPacketContext? {
        if (length < 28) return null // 20 bytes IPv4 + 8 bytes UDP

        val buffer = ByteBuffer.wrap(packet, 0, length)
        val versionAndIhl = buffer.get(0).toInt() and 0xFF
        val version = versionAndIhl shr 4
        val ihl = (versionAndIhl and 0x0F) * 4

        if (version != 4 || ihl < 20 || length < ihl + 8) return null

        val protocol = buffer.get(9).toInt() and 0xFF
        if (protocol != 17) return null // Protocol 17 = UDP

        val srcIp = ByteArray(4)
        val dstIp = ByteArray(4)
        buffer.position(12)
        buffer.get(srcIp)
        buffer.get(dstIp)

        // UDP Header starts after IP header (IHL)
        buffer.position(ihl)
        val srcPort = buffer.short.toInt() and 0xFFFF
        val dstPort = buffer.short.toInt() and 0xFFFF
        val udpLength = buffer.short.toInt() and 0xFFFF

        if (dstPort != 53 || length < ihl + udpLength) return null

        val payloadLength = udpLength - 8
        if (payloadLength <= 0) return null

        val dnsPayload = ByteArray(payloadLength)
        buffer.position(ihl + 8)
        buffer.get(dnsPayload)

        return DnsPacketContext(
            clientIp = srcIp,
            serverIp = dstIp,
            clientPort = srcPort,
            dnsPayload = dnsPayload
        )
    }

    /**
     * Builds a complete IPv4 + UDP packet wrapping a DNS response payload.
     */
    fun buildUdpResponsePacket(
        context: DnsPacketContext,
        dnsResponsePayload: ByteArray
    ): ByteArray {
        val ipHeaderLength = 20
        val udpHeaderLength = 8
        val totalLength = ipHeaderLength + udpHeaderLength + dnsResponsePayload.size

        val packet = ByteArray(totalLength)
        val buffer = ByteBuffer.wrap(packet)

        // 1. IPv4 Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        buffer.put(0x00.toByte()) // ToS
        buffer.putShort(totalLength.toShort())
        buffer.putShort(0.toShort()) // Identification
        buffer.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buffer.put(64.toByte()) // TTL
        buffer.put(17.toByte()) // Protocol 17 (UDP)
        buffer.putShort(0.toShort()) // Checksum (0 before calculation)

        // Swap IPs: server is source, client is destination
        buffer.put(context.serverIp)
        buffer.put(context.clientIp)

        // Compute IP checksum
        val ipChecksum = computeChecksum(packet, 0, ipHeaderLength)
        buffer.putShort(10, ipChecksum.toShort())

        // 2. UDP Header
        buffer.position(ipHeaderLength)
        buffer.putShort(53.toShort()) // Source Port: 53 (DNS)
        buffer.putShort(context.clientPort.toShort()) // Dest Port: client
        buffer.putShort((udpHeaderLength + dnsResponsePayload.size).toShort())
        buffer.putShort(0.toShort()) // UDP Checksum (0 = optional in IPv4)

        // 3. DNS Payload
        buffer.put(dnsResponsePayload)

        return packet
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            val high = data[i].toInt() and 0xFF
            val low = data[i + 1].toInt() and 0xFF
            sum += (high shl 8) or low
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }

    data class DnsPacketContext(
        val clientIp: ByteArray,
        val serverIp: ByteArray,
        val clientPort: Int,
        val dnsPayload: ByteArray
    )
}
