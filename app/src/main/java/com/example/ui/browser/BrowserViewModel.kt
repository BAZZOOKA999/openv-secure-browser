package com.example.ui.browser

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DnsConfiguration
import com.example.data.model.DnsProvider
import com.example.data.model.SearchEngine
import com.example.data.model.TabItem
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnProfile
import com.example.data.repository.DnsRepository
import com.example.data.repository.DnsRepositoryImpl
import com.example.data.repository.VpnRepository
import com.example.data.repository.VpnRepositoryImpl
import com.example.data.repository.VpnSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

data class ConnectivityDiagnostic(
    val hasInternet: Boolean = false,
    val transportType: String = "Desconhecido",
    val isMetered: Boolean = false,
    val dnsResolved: Boolean = false,
    val localIp: String = "Indisponível",
    val testedAt: Long = System.currentTimeMillis()
)

class BrowserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val bookmarkDao = db.bookmarkDao()
    private val historyDao = db.historyDao()
    private val prefsRepo = UserPreferencesRepository(application)

    val tabManager = TabManager()
    val tabs: StateFlow<List<TabItem>> = tabManager.tabs
    val activeTabId: StateFlow<String> = tabManager.activeTabId

    val activeTab: StateFlow<TabItem?> = combine(tabs, activeTabId) { tabList, currentId ->
        tabList.find { it.id == currentId } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tabManager.getActiveTab())

    val searchEngine: StateFlow<SearchEngine> = prefsRepo.searchEngineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchEngine.DUCKDUCKGO)

    val safeBrowsingEnabled: StateFlow<Boolean> = prefsRepo.safeBrowsingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val blockThirdPartyCookies: StateFlow<Boolean> = prefsRepo.blockThirdPartyCookiesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val desktopModeDefault: StateFlow<Boolean> = prefsRepo.desktopModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val darkThemeEnabled: StateFlow<Boolean> = prefsRepo.darkThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // VPN architecture & contracts
    val vpnRepository: VpnRepository = VpnRepositoryImpl()
    val vpnSessionManager = VpnSessionManager(vpnRepository)
    val vpnState: StateFlow<VpnConnectionState> = vpnRepository.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VpnConnectionState.NOT_CONFIGURED)
    val vpnProfiles: StateFlow<List<VpnProfile>> = vpnRepository.availableProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val activeVpnProfile: StateFlow<VpnProfile?> = vpnRepository.activeProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // DNS architecture & contracts
    val dnsRepository: DnsRepository = DnsRepositoryImpl(prefsRepo)
    val dnsConfiguration: StateFlow<DnsConfiguration> = dnsRepository.configuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DnsConfiguration())

    // Bookmarks and History
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Diagnostics state
    private val _diagnosticState = MutableStateFlow<ConnectivityDiagnostic?>(null)
    val diagnosticState: StateFlow<ConnectivityDiagnostic?> = _diagnosticState.asStateFlow()

    // Navigation trigger event to coordinate with active WebView
    private val _loadUrlEvent = MutableStateFlow<String?>(null)
    val loadUrlEvent: StateFlow<String?> = _loadUrlEvent.asStateFlow()

    fun clearLoadUrlEvent() {
        _loadUrlEvent.value = null
    }

    fun navigateTo(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        val urlToLoad = formatInputToUrl(trimmed, searchEngine.value)
        tabManager.updateActiveTab { it.copy(url = urlToLoad, errorMessage = null) }
        _loadUrlEvent.value = urlToLoad
    }

    private fun formatInputToUrl(input: String, currentSearchEngine: SearchEngine): String {
        if (input == "openv://home" || input == "about:blank") {
            return input
        }
        val lower = input.lowercase()
        // If it starts with a protocol
        if (lower.startsWith("https://") || lower.startsWith("http://")) {
            return input
        }
        // If it looks like a domain name (contains a dot, no spaces)
        val hasSpaces = input.contains(" ")
        val hasDot = input.contains(".")
        if (!hasSpaces && hasDot && input.length >= 4) {
            return "https://$input"
        }
        // Otherwise treat as search query
        return currentSearchEngine.buildQueryUrl(input)
    }

    fun openNewTab(url: String = "openv://home", isPrivate: Boolean = false) {
        tabManager.openNewTab(url, isPrivate)
        _loadUrlEvent.value = url
        vpnSessionManager.rotateSession()
    }

    fun selectTab(tabId: String) {
        tabManager.selectTab(tabId)
    }

    fun closeTab(tabId: String) {
        tabManager.closeTab(tabId)
    }

    fun closeAllTabs(keepOnePrivate: Boolean = false) {
        tabManager.closeAllTabs(keepOnePrivate)
    }

    fun goHome() {
        tabManager.updateActiveTab { it.copy(url = "openv://home", errorMessage = null) }
        _loadUrlEvent.value = "openv://home"
    }

    fun toggleDesktopMode() {
        val current = activeTab.value?.isDesktopMode ?: false
        val updated = !current
        tabManager.updateActiveTab { it.copy(isDesktopMode = updated) }
    }

    fun recordVisit(title: String, url: String, isPrivate: Boolean) {
        if (isPrivate || url.startsWith("openv://") || url.startsWith("about:")) return
        viewModelScope.launch(Dispatchers.IO) {
            val cleanTitle = if (title.isBlank() || title == url) url else title
            historyDao.insertHistory(
                HistoryEntity(
                    title = cleanTitle,
                    url = url,
                    visitedAt = System.currentTimeMillis(),
                    isPrivate = false
                )
            )
        }
    }

    fun toggleBookmark(title: String, url: String) {
        if (url.startsWith("openv://") || url.startsWith("about:")) return
        viewModelScope.launch(Dispatchers.IO) {
            val isBookmarked = bookmarks.value.any { it.url == url }
            if (isBookmarked) {
                bookmarkDao.deleteByUrl(url)
            } else {
                val cleanTitle = if (title.isBlank()) url else title
                bookmarkDao.insertBookmark(
                    BookmarkEntity(title = cleanTitle, url = url)
                )
            }
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            bookmarkDao.deleteBookmark(bookmark)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            historyDao.clearAllHistory()
        }
    }

    fun clearBrowsingData(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Clear cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()

                // Clear web storage (localStorage, sessionStorage)
                WebStorage.getInstance().deleteAllData()

                // Clear WebView cache
                val dummyWebView = WebView(context)
                dummyWebView.clearCache(true)
                dummyWebView.destroy()
            } catch (_: Exception) {}

            withContext(Dispatchers.IO) {
                historyDao.clearAllHistory()
            }
            onComplete()
        }
    }

    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch {
            prefsRepo.setSearchEngine(engine)
        }
    }

    fun setDnsProvider(provider: DnsProvider) {
        viewModelScope.launch {
            dnsRepository.setProvider(provider)
        }
    }

    fun setCustomDnsUrl(url: String) {
        viewModelScope.launch {
            dnsRepository.setCustomDohUrl(url)
        }
    }

    fun setSafeBrowsing(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setSafeBrowsing(enabled)
        }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setBlockThirdPartyCookies(enabled)
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setDarkTheme(enabled)
        }
    }

    fun runConnectivityDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNet = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNet)

            val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            val transport = when {
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "Wi-Fi"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Rede Móvel"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN Ativa"
                else -> "Sem conexão"
            }

            val isMetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == false

            // Real DNS resolution check (honest, tests Cloudflare/Quad9 root)
            val dnsResolved = try {
                val address = InetAddress.getByName("one.one.one.one")
                address.hostAddress != null
            } catch (_: Exception) {
                false
            }

            val localIp = try {
                val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                var ip = "Indisponível"
                for (inter in interfaces) {
                    for (addr in inter.inetAddresses) {
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            ip = addr.hostAddress ?: "Indisponível"
                            break
                        }
                    }
                }
                ip
            } catch (_: Exception) {
                "Indisponível"
            }

            _diagnosticState.value = ConnectivityDiagnostic(
                hasInternet = hasInternet,
                transportType = transport,
                isMetered = isMetered,
                dnsResolved = dnsResolved,
                localIp = localIp,
                testedAt = System.currentTimeMillis()
            )
        }
    }
}
