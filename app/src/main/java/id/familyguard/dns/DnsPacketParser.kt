package id.familyguard.dns

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

object DnsPacketParser {

    const val TYPE_A = 1
    const val TYPE_AAAA = 28
    const val CLASS_IN = 1

    data class DnsQuery(
        val transactionId: Int,
        val domain: String,
        val qType: Int,
        val qClass: Int
    )

    /**
     * Parses the DNS payload from raw bytes (RFC 1035).
     * Returns null if the packet is malformed or not a query.
     */
    fun parseQuery(packet: ByteArray): DnsQuery? {
        if (packet.size < 12) return null

        val buffer = ByteBuffer.wrap(packet)
        val transactionId = buffer.short.toInt() and 0xFFFF
        val flags = buffer.short.toInt() and 0xFFFF

        // Only process standard queries (QR = 0, Opcode = 0)
        val isResponse = (flags and 0x8000) != 0
        val opcode = (flags and 0x7800) shr 11
        if (isResponse || opcode != 0) return null

        val qdCount = buffer.short.toInt() and 0xFFFF
        if (qdCount < 1) return null

        // Skip ANCOUNT (2 bytes), NSCOUNT (2 bytes), and ARCOUNT (2 bytes) to reach Question section
        buffer.position(12)

        // Parse Question section
        val domainBuilder = StringBuilder()
        while (buffer.hasRemaining()) {
            val labelLength = buffer.get().toInt() and 0xFF
            if (labelLength == 0) break // Terminating null byte

            if ((labelLength and 0xC0) == 0xC0) {
                // Compression pointer
                if (!buffer.hasRemaining()) return null
                buffer.get() // Skip pointer offset byte
                break
            }

            if (buffer.remaining() < labelLength) return null
            val labelBytes = ByteArray(labelLength)
            buffer.get(labelBytes)

            if (domainBuilder.isNotEmpty()) {
                domainBuilder.append('.')
            }
            domainBuilder.append(String(labelBytes, Charsets.US_ASCII))
        }

        if (buffer.remaining() < 4) return null
        val qType = buffer.short.toInt() and 0xFFFF
        val qClass = buffer.short.toInt() and 0xFFFF

        val domain = domainBuilder.toString().lowercase()
        if (domain.isEmpty()) return null

        return DnsQuery(transactionId, domain, qType, qClass)
    }

    /**
     * Synthesizes an instant 0.0.0.0 (or ::) sinkhole DNS response packet.
     */
    fun buildSinkholeResponse(query: DnsQuery, ttlSeconds: Int = 300): ByteArray {
        val out = ByteArrayOutputStream()
        val data = DataOutputStream(out)

        // 1. Transaction ID
        data.writeShort(query.transactionId)

        // 2. Flags: Standard Response, NoError (0x8180: QR=1, RD=1, RA=1)
        data.writeShort(0x8180)

        // 3. Counts: 1 Question, 1 Answer, 0 Authority, 0 Additional
        data.writeShort(1) // QDCOUNT
        data.writeShort(1) // ANCOUNT
        data.writeShort(0) // NSCOUNT
        data.writeShort(0) // ARCOUNT

        // 4. Question Section
        writeDomainLabels(data, query.domain)
        data.writeShort(query.qType)
        data.writeShort(query.qClass)

        // 5. Answer Section
        // Name pointer to question domain (0xC00C points to byte offset 12)
        data.writeShort(0xC00C)
        data.writeShort(query.qType)
        data.writeShort(query.qClass)
        data.writeInt(ttlSeconds)

        when (query.qType) {
            TYPE_AAAA -> {
                data.writeShort(16) // RDLENGTH for IPv6
                data.write(ByteArray(16)) // 16 bytes of 0 (::)
            }
            else -> {
                data.writeShort(4) // RDLENGTH for IPv4
                data.write(byteArrayOf(0, 0, 0, 0)) // 0.0.0.0
            }
        }

        data.flush()
        return out.toByteArray()
    }

    private fun writeDomainLabels(data: DataOutputStream, domain: String) {
        val parts = domain.split('.')
        for (part in parts) {
            val bytes = part.toByteArray(Charsets.US_ASCII)
            data.writeByte(bytes.size)
            data.write(bytes)
        }
        data.writeByte(0) // Root label
    }
}
