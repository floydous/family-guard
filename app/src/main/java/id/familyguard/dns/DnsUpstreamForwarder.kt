package id.familyguard.dns

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsUpstreamForwarder {

    private const val TAG = "DnsUpstreamForwarder"
    private const val SOCKET_TIMEOUT_MS = 2500

    // High-speed public resolvers (Cloudflare 1.1.1.1, 1.0.0.1, Google 8.8.8.8, 8.8.4.4, Quad9 9.9.9.9)
    private val UPSTREAM_RESOLVERS = listOf(
        InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
        InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
        InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8)),
        InetAddress.getByAddress(byteArrayOf(8, 8, 4, 4)),
        InetAddress.getByAddress(byteArrayOf(9, 9, 9, 9))
    )

    /**
     * Forwards an unblocked DNS query to physical network DNS or high-speed public resolvers.
     * vpnService.protect(socket) ensures packets bypass the VPN interface and exit over physical Wi-Fi/Cellular.
     */
    suspend fun forward(
        vpnService: VpnService,
        queryPayload: ByteArray,
        extraDnsServers: List<InetAddress> = emptyList()
    ): ByteArray? = withContext(Dispatchers.IO) {
        // Try local network DNS servers first if available
        for (server in extraDnsServers) {
            val response = queryServer(vpnService, server, queryPayload)
            if (response != null && response.isNotEmpty()) {
                return@withContext response
            }
        }

        // Fallback to primary public resolvers
        for (server in UPSTREAM_RESOLVERS) {
            val response = queryServer(vpnService, server, queryPayload)
            if (response != null && response.isNotEmpty()) {
                return@withContext response
            }
        }

        Log.w(TAG, "Upstream DNS resolution timed out for query of size ${queryPayload.size}")
        null
    }

    private fun queryServer(
        vpnService: VpnService,
        serverAddress: InetAddress,
        queryPayload: ByteArray
    ): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()
            // Protect socket so its outbound packets are routed over physical network, never into tun0
            vpnService.protect(socket)
            socket.soTimeout = SOCKET_TIMEOUT_MS

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
