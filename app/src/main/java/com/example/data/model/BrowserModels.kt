package com.example.data.model

import java.util.UUID

enum class SearchEngine(
    val displayName: String,
    val searchUrl: String,
    val imageSearchUrl: String,
    val newsSearchUrl: String,
    val iconText: String
) {
    DUCKDUCKGO(
        "DuckDuckGo",
        "https://duckduckgo.com/?q=",
        "https://duckduckgo.com/?iax=images&ia=images&q=",
        "https://duckduckgo.com/?iar=news&ia=news&q=",
        "DDG"
    ),
    GOOGLE(
        "Google",
        "https://www.google.com/search?q=",
        "https://www.google.com/search?tbm=isch&q=",
        "https://www.google.com/search?tbm=nws&q=",
        "G"
    ),
    BRAVE(
        "Brave Search",
        "https://search.brave.com/search?q=",
        "https://search.brave.com/images?q=",
        "https://search.brave.com/news?q=",
        "BRV"
    ),
    STARTPAGE(
        "Startpage",
        "https://www.startpage.com/sp/search?query=",
        "https://www.startpage.com/sp/search?cat=images&query=",
        "https://www.startpage.com/sp/search?cat=news&query=",
        "SP"
    ),
    BING(
        "Bing",
        "https://www.bing.com/search?q=",
        "https://www.bing.com/images/search?q=",
        "https://www.bing.com/news/search?q=",
        "BNG"
    ),
    WIKIPEDIA(
        "Wikipedia",
        "https://pt.wikipedia.org/w/index.php?search=",
        "https://commons.wikimedia.org/w/index.php?search=",
        "https://pt.wikipedia.org/w/index.php?search=",
        "WIK"
    ),
    GITHUB(
        "GitHub",
        "https://github.com/search?q=",
        "https://github.com/search?type=repositories&q=",
        "https://github.com/search?type=commits&q=",
        "GH"
    );

    fun buildQueryUrl(query: String, category: SearchCategory = SearchCategory.WEB): String {
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        return when (category) {
            SearchCategory.WEB -> searchUrl + encoded
            SearchCategory.IMAGES -> imageSearchUrl + encoded
            SearchCategory.VIDEOS -> searchUrl + java.net.URLEncoder.encode("$query video", "UTF-8")
            SearchCategory.NEWS -> newsSearchUrl + encoded
            SearchCategory.DOCUMENTS -> searchUrl + java.net.URLEncoder.encode("$query filetype:pdf OR filetype:doc", "UTF-8")
            SearchCategory.CODE -> if (this == GITHUB) searchUrl + encoded else searchUrl + java.net.URLEncoder.encode("site:github.com $query", "UTF-8")
            SearchCategory.OSINT -> searchUrl + java.net.URLEncoder.encode("site:github.com OR site:pastebin.com OR site:archive.org $query", "UTF-8")
        }
    }
}

enum class SearchCategory(val labelPt: String, val description: String) {
    WEB("Web", "Busca geral em toda a web"),
    IMAGES("Imagens", "Pesquisa reversa e imagens"),
    VIDEOS("Vídeos", "Mídias audiovisuais"),
    NEWS("Notícias", "Artigos e atualizações recentes"),
    DOCUMENTS("Documentos", "Arquivos PDF, DOC, XLS expostos"),
    CODE("Código", "Repositórios e trechos de código"),
    OSINT("OSINT", "Bases públicas, arquivos e pastes")
}

data class TabItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "openv://home",
    val title: String = "Página Inicial",
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isPrivate: Boolean = false,
    val isDesktopMode: Boolean = false,
    val lastAccessed: Long = System.currentTimeMillis(),
    val isSuspended: Boolean = false,
    val sslCertificateValid: Boolean? = null,
    val errorMessage: String? = null
)

// VPN Architecture models
enum class VpnConnectionState(val labelPt: String) {
    NOT_CONFIGURED("Não configurada"),
    DISCONNECTED("Desconectado"),
    CONNECTING("Conectando..."),
    CONNECTED("Conectado"),
    RECONNECTING("Reconectando..."),
    ERROR("Erro na conexão")
}

enum class VpnProtocol(val displayName: String) {
    WIREGUARD("WireGuard"),
    OPENVPN_UDP("OpenVPN (UDP)"),
    OPENVPN_TCP("OpenVPN (TCP)"),
    SYSTEM_TUN("Túnel Nativo Android VpnService")
}

data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serverHost: String,
    val serverPort: Int = 51820,
    val protocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val countryCode: String = "BR",
    val username: String = "",
    val isConfigured: Boolean = true,
    val dnsServer: String = "1.1.1.1"
)

// DNS Architecture models
enum class DnsProvider(
    val displayName: String,
    val primaryAddress: String,
    val dohUrl: String,
    val description: String
) {
    CLOUDFLARE(
        "Cloudflare (1.1.1.1)",
        "1.1.1.1",
        "https://cloudflare-dns.com/dns-query",
        "Alta velocidade e bloqueio de rastreamento"
    ),
    QUAD9(
        "Quad9 (9.9.9.9)",
        "9.9.9.9",
        "https://dns.quad9.net/dns-query",
        "Foco estrito em segurança e bloqueio de malware"
    ),
    GOOGLE_PUBLIC(
        "Google Public DNS",
        "8.8.8.8",
        "https://dns.google/resolve",
        "Resolução estável e ampla cobertura global"
    ),
    CUSTOM(
        "DNS Personalizado (DoH)",
        "",
        "",
        "Servidor DNS-over-HTTPS configurado pelo usuário"
    )
}

enum class DnsProtectionState(val labelPt: String) {
    NOT_APPLIED("Não aplicado"),
    OPERATIONAL("Operacional no cliente DoH"),
    ACTIVE("Ativo")
}

data class DnsConfiguration(
    val provider: DnsProvider = DnsProvider.CLOUDFLARE,
    val customDohUrl: String = "",
    val state: DnsProtectionState = DnsProtectionState.OPERATIONAL
)

// Public IP & Telemetry Info (real detection)
data class PublicIpInfo(
    val ip: String,
    val country: String = "Desconhecido",
    val countryCode: String = "",
    val city: String = "",
    val isp: String = "",
    val org: String = "",
    val asNumber: String = "",
    val queryTimestamp: Long = System.currentTimeMillis()
)

// DNS record result for OSINT
data class DnsRecordItem(
    val type: String,
    val name: String,
    val data: String,
    val ttl: Int
)

// OSINT Models
data class OsintToolItem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val urlTemplate: String,
    val isReady: Boolean = true,
    val placeholderHint: String = "Digite o alvo..."
)

// Download item tracking
data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val status: String = "Concluído",
    val timestamp: Long = System.currentTimeMillis()
)
