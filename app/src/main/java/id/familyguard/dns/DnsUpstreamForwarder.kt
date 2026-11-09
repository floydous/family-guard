package id.familyguard.dns

import android.net.ConnectivityManager
import android.net.Network
import android.net.VpnService
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object DnsUpstreamForwarder {

    private const val TAG = "DnsUpstreamForwarder"
    private const val SOCKET_TIMEOUT_MS = 2500

    // Standard public fallback resolvers (RFC 6890 / Cloudflare & Google)
    private val FALLBACK_RESOLVERS = listOf(
        InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)),
        InetAddress.getByAddress(byteArrayOf(1, 0, 0, 1)),
        InetAddress.getByAddress(byteArrayOf(8, 8, 8, 8))
    )

    /**
     * Forwards an unblocked DNS query to the underlying physical network DNS servers,
     * falling back to secure public resolvers if the local link servers fail or are unavailable.
     */
    suspend fun forward(
        vpnService: VpnService,
        queryPayload: ByteArray,
        discoveredDnsServers: List<InetAddress> = emptyList()
    ): ByteArray? = withContext(Dispatchers.IO) {
        val cm = vpnService.getSystemService(ConnectivityManager::class.java)
        val underlyingNetwork = cm?.activeNetwork

        // 1. Try dynamically discovered DNS servers from the physical network (Wi-Fi router / Mobile carrier)
        for (server in discoveredDnsServers) {
            val response = queryServer(vpnService, underlyingNetwork, server, queryPayload)
            if (response != null && response.isNotEmpty()) {
                return@withContext response
            }
        }

        // 2. Fall back to standard public resolvers if network DNS didn't respond
        for (server in FALLBACK_RESOLVERS) {
            val response = queryServer(vpnService, underlyingNetwork, server, queryPayload)
            if (response != null && response.isNotEmpty()) {
                return@withContext response
            }
        }

        Log.w(TAG, "All upstream DNS resolvers failed to respond for query of size ${queryPayload.size}")
        null
    }

    private fun queryServer(
        vpnService: VpnService,
        underlyingNetwork: Network?,
        serverAddress: InetAddress,
        queryPayload: ByteArray
    ): ByteArray? {
        var socket: DatagramSocket? = null
        return try {
            socket = DatagramSocket()

            // Bind to underlying physical network on Android 6+ to bypass VPN routing
            if (underlyingNetwork != null) {
                underlyingNetwork.bindSocket(socket)
            }
            // Protect socket on VpnService so its packets are excluded from the tun interface
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
