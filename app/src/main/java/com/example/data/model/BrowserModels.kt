package com.example.data.model

import java.util.UUID

enum class SearchEngine(val displayName: String, val searchUrl: String, val iconText: String) {
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q=", "DDG"),
    GOOGLE("Google", "https://www.google.com/search?q=", "G"),
    BRAVE("Brave Search", "https://search.brave.com/search?q=", "BRV"),
    BING("Bing", "https://www.bing.com/search?q=", "BNG");

    fun buildQueryUrl(query: String): String {
        return searchUrl + java.net.URLEncoder.encode(query, "UTF-8")
    }
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
    WIREGUARD("WireGuard (Recomendado)"),
    OPENVPN_UDP("OpenVPN UDP"),
    OPENVPN_TCP("OpenVPN TCP")
}

data class VpnProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val serverHost: String,
    val serverPort: Int,
    val protocol: VpnProtocol = VpnProtocol.WIREGUARD,
    val countryCode: String = "BR",
    val isConfigured: Boolean = false
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
        "https://dns.google/dns-query",
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
    NOT_APPLIED("Não aplicado no WebView"),
    PERSISTED_FOR_STAGE_2("Configurado (Ativação na Etapa 2)"),
    ACTIVE("Ativo")
}

data class DnsConfiguration(
    val provider: DnsProvider = DnsProvider.CLOUDFLARE,
    val customDohUrl: String = "",
    val state: DnsProtectionState = DnsProtectionState.NOT_APPLIED
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
