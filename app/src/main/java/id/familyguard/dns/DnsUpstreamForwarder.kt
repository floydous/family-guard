package id.familyguard.dns

import android.net.VpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsUpstreamForwarder {

    // Upstream DNS servers: Cloudflare (1.1.1.1, 1.0.0.1) and Google (8.8.8.8)
    // Using literal byte arrays guarantees ZERO DNS lookups when constructing InetAddress
    private val UPSTREAM_SERVERS = listOf(
        byteArrayOf(1, 1, 1, 1),
        byteArrayOf(1, 0, 0, 1),
        byteArrayOf(8, 8, 8, 8)
    )

    suspend fun forward(vpnService: VpnService, queryPayload: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        for (serverBytes in UPSTREAM_SERVERS) {
            val response = queryServer(vpnService, serverBytes, queryPayload)
            if (response != null && response.isNotEmpty()) {
                return@withContext response
            }
        }
        null
    }

    private fun queryServer(vpnService: VpnService, serverIpBytes: ByteArray, queryPayload: ByteArray): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            // CRUCIAL: Protect socket so its outbound UDP packets bypass the VPN interface!
            vpnService.protect(socket)
            socket.soTimeout = 2500

            val serverAddress = InetAddress.getByAddress(serverIpBytes)
            val sendPacket = DatagramPacket(queryPayload, queryPayload.size, serverAddress, 53)
            socket.send(sendPacket)

            val receiveBuf = ByteArray(2048)
            val receivePacket = DatagramPacket(receiveBuf, receiveBuf.size)
            socket.receive(receivePacket)

            receiveBuf.copyOf(receivePacket.length)
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (ignored: Exception) {}
        }
    }
}
