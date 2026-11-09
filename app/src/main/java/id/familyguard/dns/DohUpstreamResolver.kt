package id.familyguard.dns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class DohUpstreamResolver(
    private val endpointUrl: String = DEFAULT_CLOUDFLARE_DOH
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()

    private val dnsMediaType = "application/dns-message".toMediaType()

    /**
     * Forwards an unblocked DNS query to the secure encrypted upstream DoH resolver (RFC 8484).
     */
    suspend fun resolve(queryBytes: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        val requestBody = queryBytes.toRequestBody(dnsMediaType)
        val request = Request.Builder()
            .url(endpointUrl)
            .post(requestBody)
            .header("Accept", "application/dns-message")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    return@withContext response.body?.bytes()
                }
            }
        } catch (e: IOException) {
            // Network timeout or unreachable
        }
        return@withContext null
    }

    companion object {
        const val DEFAULT_CLOUDFLARE_DOH = "https://cloudflare-dns.com/dns-query"
        const val QUAD9_DOH = "https://dns.quad9.net/dns-query"
    }
}
