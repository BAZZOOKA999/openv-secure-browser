package com.example.network

import com.example.data.model.PublicIpInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HttpHeaderInspectionResult(
    val url: String,
    val finalUrl: String,
    val statusCode: Int,
    val redirectChain: List<String>,
    val headers: Map<String, String>,
    val securityHeaders: Map<String, Boolean>,
    val isHttps: Boolean,
    val latencyMs: Long
)

data class SocialProfileCheckResult(
    val platform: String,
    val profileUrl: String,
    val exists: Boolean,
    val checkedStatusCode: Int
)

class NetworkSecurityClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val directClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetchPublicIpInfo(): Result<PublicIpInfo> = withContext(Dispatchers.IO) {
        try {
            // First get public IP
            val ipRequest = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "OpenV-SecureBrowser/0.1")
                .build()

            val ipJson = directClient.newCall(ipRequest).execute().use { res ->
                if (!res.isSuccessful) throw Exception("HTTP ${res.code}")
                JSONObject(res.body?.string() ?: "{}")
            }
            val ip = ipJson.optString("ip", "")
            if (ip.isBlank()) throw Exception("IP vazio recebido")

            // Geolocation and ASN via ip-api
            val geoRequest = Request.Builder()
                .url("http://ip-api.com/json/$ip")
                .header("User-Agent", "OpenV-SecureBrowser/0.1")
                .build()

            val geoJson = directClient.newCall(geoRequest).execute().use { res ->
                if (res.isSuccessful) JSONObject(res.body?.string() ?: "{}") else JSONObject()
            }

            val country = geoJson.optString("country", "Desconhecido")
            val countryCode = geoJson.optString("countryCode", "")
            val city = geoJson.optString("city", "")
            val isp = geoJson.optString("isp", "")
            val org = geoJson.optString("org", "")
            val asNumber = geoJson.optString("as", "")

            Result.success(
                PublicIpInfo(
                    ip = ip,
                    country = country,
                    countryCode = countryCode,
                    city = city,
                    isp = isp,
                    org = org,
                    asNumber = asNumber
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun inspectHttpHeadersAndRedirects(initialUrl: String): Result<HttpHeaderInspectionResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            var currentUrl = if (!initialUrl.startsWith("http://") && !initialUrl.startsWith("https://")) {
                "https://$initialUrl"
            } else initialUrl

            val redirectChain = mutableListOf<String>()
            val maxHops = 6
            var hops = 0
            var finalStatus = 0
            val allHeaders = mutableMapOf<String, String>()

            try {
                while (hops < maxHops) {
                    redirectChain.add(currentUrl)
                    val request = Request.Builder()
                        .url(currentUrl)
                        .header("User-Agent", "Mozilla/5.0 (Android; OpenV Secure Browser)")
                        .build()

                    val (statusCode, nextUrl, headersMap) = httpClient.newCall(request).execute().use { response ->
                        finalStatus = response.code
                        val location = response.header("Location")
                        val map = mutableMapOf<String, String>()
                        for (i in 0 until response.headers.size) {
                            map[response.headers.name(i).lowercase()] = response.headers.value(i)
                        }
                        Triple(response.code, location, map)
                    }

                    allHeaders.clear()
                    allHeaders.putAll(headersMap)

                    if (statusCode in 300..399 && !nextUrl.isNullOrBlank()) {
                        currentUrl = if (nextUrl.startsWith("http")) nextUrl else java.net.URI(currentUrl).resolve(nextUrl).toString()
                        hops++
                    } else {
                        break
                    }
                }

                val latency = System.currentTimeMillis() - startTime
                val isHttps = currentUrl.startsWith("https://")

                val securityHeaders = mapOf(
                    "HSTS (Strict-Transport-Security)" to allHeaders.containsKey("strict-transport-security"),
                    "CSP (Content-Security-Policy)" to allHeaders.containsKey("content-security-policy"),
                    "X-Frame-Options (Anti-Clickjacking)" to allHeaders.containsKey("x-frame-options"),
                    "X-Content-Type-Options" to allHeaders.containsKey("x-content-type-options"),
                    "Referrer-Policy" to allHeaders.containsKey("referrer-policy")
                )

                Result.success(
                    HttpHeaderInspectionResult(
                        url = initialUrl,
                        finalUrl = currentUrl,
                        statusCode = finalStatus,
                        redirectChain = redirectChain,
                        headers = allHeaders,
                        securityHeaders = securityHeaders,
                        isHttps = isHttps,
                        latencyMs = latency
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun checkUsernameProfiles(username: String): List<SocialProfileCheckResult> =
        withContext(Dispatchers.IO) {
            val cleanUser = username.trim().removePrefix("@")
            val platforms = listOf(
                Pair("GitHub", "https://github.com/$cleanUser"),
                Pair("Reddit", "https://www.reddit.com/user/$cleanUser/about.json"),
                Pair("HackerNews", "https://news.ycombinator.com/user?id=$cleanUser"),
                Pair("Telegram", "https://t.me/$cleanUser"),
                Pair("X / Twitter", "https://x.com/$cleanUser"),
                Pair("GitLab", "https://gitlab.com/$cleanUser"),
                Pair("Wikipedia User", "https://pt.wikipedia.org/wiki/Usu%C3%A1rio:$cleanUser")
            )

            platforms.map { (platform, url) ->
                async {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                            .build()

                        directClient.newCall(request).execute().use { response ->
                            val exists = response.code == 200
                            SocialProfileCheckResult(
                                platform = platform,
                                profileUrl = if (platform == "Reddit") "https://www.reddit.com/user/$cleanUser" else url,
                                exists = exists,
                                checkedStatusCode = response.code
                            )
                        }
                    } catch (e: Exception) {
                        SocialProfileCheckResult(
                            platform = platform,
                            profileUrl = url,
                            exists = false,
                            checkedStatusCode = 0
                        )
                    }
                }
            }.awaitAll()
        }

    suspend fun lookupRdap(target: String): Result<String> = withContext(Dispatchers.IO) {
        val clean = target.trim().removePrefix("https://").removePrefix("http://").split("/")[0]
        try {
            val url = if (clean.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                "https://rdap.arin.net/registry/ip/$clean"
            } else {
                "https://rdap.arin.net/registry/ip/$clean"
            }

            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/rdap+json, application/json")
                .header("User-Agent", "OpenV-SecureBrowser/0.1")
                .build()

            directClient.newCall(request).execute().use { res ->
                if (res.isSuccessful) {
                    Result.success(res.body?.string() ?: "{}")
                } else {
                    Result.failure(Exception("RDAP HTTP ${res.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
