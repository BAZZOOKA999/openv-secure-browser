package com.example.network

import com.example.data.model.DnsProvider
import com.example.data.model.DnsRecordItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DoHQueryResult(
    val domain: String,
    val recordType: String,
    val provider: String,
    val latencyMs: Long,
    val records: List<DnsRecordItem>,
    val statusText: String,
    val isSuccess: Boolean
)

class DnsOverHttpsClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun resolve(
        domain: String,
        recordType: String = "A",
        provider: DnsProvider = DnsProvider.CLOUDFLARE,
        customDohUrl: String = ""
    ): DoHQueryResult = withContext(Dispatchers.IO) {
        val cleanDomain = domain.trim().removePrefix("https://").removePrefix("http://").split("/")[0]
        val startTime = System.currentTimeMillis()

        val endpoint = when (provider) {
            DnsProvider.CLOUDFLARE -> "https://cloudflare-dns.com/dns-query?name=$cleanDomain&type=$recordType"
            DnsProvider.GOOGLE_PUBLIC -> "https://dns.google/resolve?name=$cleanDomain&type=$recordType"
            DnsProvider.QUAD9 -> "https://dns.quad9.net:5053/dns-query?name=$cleanDomain&type=$recordType"
            DnsProvider.CUSTOM -> {
                if (customDohUrl.isBlank()) "https://cloudflare-dns.com/dns-query?name=$cleanDomain&type=$recordType"
                else if (customDohUrl.contains("?")) "$customDohUrl&name=$cleanDomain&type=$recordType"
                else "$customDohUrl?name=$cleanDomain&type=$recordType"
            }
        }

        try {
            val request = Request.Builder()
                .url(endpoint)
                .header("Accept", "application/dns-json")
                .header("User-Agent", "OpenV-SecureBrowser/0.1")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                if (!response.isSuccessful) {
                    return@withContext DoHQueryResult(
                        domain = cleanDomain,
                        recordType = recordType,
                        provider = provider.displayName,
                        latencyMs = latency,
                        records = emptyList(),
                        statusText = "HTTP Erro: ${response.code}",
                        isSuccess = false
                    )
                }

                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val status = json.optInt("Status", 0)

                val recordsList = mutableListOf<DnsRecordItem>()
                val answersArray = json.optJSONArray("Answer")
                if (answersArray != null) {
                    for (i in 0 until answersArray.length()) {
                        val obj = answersArray.getJSONObject(i)
                        val rTypeNum = obj.optInt("type", 1)
                        val rTypeName = mapDnsType(rTypeNum)
                        val rData = obj.optString("data", "")
                        val rTtl = obj.optInt("TTL", 300)
                        val rName = obj.optString("name", cleanDomain)
                        recordsList.add(DnsRecordItem(rTypeName, rName, rData, rTtl))
                    }
                }

                val statusText = when (status) {
                    0 -> "NOERROR (Sucesso)"
                    2 -> "SERVFAIL (Falha no servidor)"
                    3 -> "NXDOMAIN (Domínio não existe)"
                    else -> "Status RCODE $status"
                }

                DoHQueryResult(
                    domain = cleanDomain,
                    recordType = recordType,
                    provider = provider.displayName,
                    latencyMs = latency,
                    records = recordsList,
                    statusText = statusText,
                    isSuccess = status == 0 && recordsList.isNotEmpty()
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            DoHQueryResult(
                domain = cleanDomain,
                recordType = recordType,
                provider = provider.displayName,
                latencyMs = latency,
                records = emptyList(),
                statusText = "Falha de conexão: ${e.localizedMessage ?: "Timeout"}",
                isSuccess = false
            )
        }
    }

    private fun mapDnsType(typeNum: Int): String {
        return when (typeNum) {
            1 -> "A"
            2 -> "NS"
            5 -> "CNAME"
            6 -> "SOA"
            15 -> "MX"
            16 -> "TXT"
            28 -> "AAAA"
            257 -> "CAA"
            else -> "TYPE_$typeNum"
        }
    }
}
