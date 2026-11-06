package com.example.ui.browser

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.BookmarkEntity
import com.example.data.local.HistoryEntity
import com.example.data.local.OsintReportEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DnsConfiguration
import com.example.data.model.DnsProvider
import com.example.data.model.DownloadItem
import com.example.data.model.PublicIpInfo
import com.example.data.model.SearchCategory
import com.example.data.model.SearchEngine
import com.example.data.model.TabItem
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnProfile
import com.example.data.repository.DnsRepository
import com.example.data.repository.DnsRepositoryImpl
import com.example.download.OpenVDownloadManager
import com.example.network.DnsOverHttpsClient
import com.example.network.DoHQueryResult
import com.example.network.HttpHeaderInspectionResult
import com.example.network.NetworkSecurityClient
import com.example.network.SocialProfileCheckResult
import com.example.security.ExifInfo
import com.example.security.ExifManager
import com.example.security.TrackerBlocker
import com.example.vpn.VpnManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
    private val osintReportDao = db.osintReportDao()
    private val prefsRepo = UserPreferencesRepository(application)

    // Security & Network Services
    val dohClient = DnsOverHttpsClient()
    val securityClient = NetworkSecurityClient()
    val exifManager = ExifManager(application)
    val downloadManager = OpenVDownloadManager(application)
    val vpnManager = VpnManager(application, viewModelScope)

    // Tab Management
    val tabManager = TabManager()
    val tabs: StateFlow<List<TabItem>> = tabManager.tabs
    val activeTabId: StateFlow<String> = tabManager.activeTabId

    val activeTab: StateFlow<TabItem?> = combine(tabs, activeTabId) { tabList, currentId ->
        tabList.find { it.id == currentId } ?: tabList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), tabManager.getActiveTab())

    // Preferences
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

    // DNS repository
    val dnsRepository: DnsRepository = DnsRepositoryImpl(prefsRepo)
    val dnsConfiguration: StateFlow<DnsConfiguration> = dnsRepository.configuration
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DnsConfiguration())

    // Bookmarks, History & OSINT Reports
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = historyDao.getRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val osintReports: StateFlow<List<OsintReportEntity>> = osintReportDao.getAllReports()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val downloads: StateFlow<List<DownloadItem>> = downloadManager.downloads

    // Diagnostics & Live Results
    private val _diagnosticState = MutableStateFlow<ConnectivityDiagnostic?>(null)
    val diagnosticState: StateFlow<ConnectivityDiagnostic?> = _diagnosticState.asStateFlow()

    private val _dohQueryResult = MutableStateFlow<DoHQueryResult?>(null)
    val dohQueryResult: StateFlow<DoHQueryResult?> = _dohQueryResult.asStateFlow()

    private val _httpInspectionResult = MutableStateFlow<HttpHeaderInspectionResult?>(null)
    val httpInspectionResult: StateFlow<HttpHeaderInspectionResult?> = _httpInspectionResult.asStateFlow()

    private val _socialCheckResults = MutableStateFlow<List<SocialProfileCheckResult>>(emptyList())
    val socialCheckResults: StateFlow<List<SocialProfileCheckResult>> = _socialCheckResults.asStateFlow()

    private val _exifResult = MutableStateFlow<ExifInfo?>(null)
    val exifResult: StateFlow<ExifInfo?> = _exifResult.asStateFlow()

    private val _sanitizedImageFile = MutableStateFlow<File?>(null)
    val sanitizedImageFile: StateFlow<File?> = _sanitizedImageFile.asStateFlow()

    private val _rdapResult = MutableStateFlow<String?>(null)
    val rdapResult: StateFlow<String?> = _rdapResult.asStateFlow()

    // Navigation trigger event to coordinate with active WebView
    private val _loadUrlEvent = MutableStateFlow<String?>(null)
    val loadUrlEvent: StateFlow<String?> = _loadUrlEvent.asStateFlow()

    // Find in Page & Zoom controls
    private val _findInPageQuery = MutableStateFlow<String?>(null)
    val findInPageQuery: StateFlow<String?> = _findInPageQuery.asStateFlow()

    private val _zoomAction = MutableStateFlow(0)
    val zoomAction: StateFlow<Int> = _zoomAction.asStateFlow()

    fun clearLoadUrlEvent() {
        _loadUrlEvent.value = null
    }

    fun setFindInPageQuery(query: String?) {
        _findInPageQuery.value = query
    }

    fun triggerZoomIn() {
        _zoomAction.value = _zoomAction.value + 1
    }

    fun triggerZoomOut() {
        _zoomAction.value = _zoomAction.value - 1
    }

    fun navigateTo(rawInput: String, category: SearchCategory = SearchCategory.WEB) {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return

        val urlToLoad = formatInputToUrl(trimmed, searchEngine.value, category)
        tabManager.updateActiveTab { it.copy(url = urlToLoad, errorMessage = null) }
        _loadUrlEvent.value = urlToLoad
    }

    private fun formatInputToUrl(input: String, currentSearchEngine: SearchEngine, category: SearchCategory): String {
        if (input == "openv://home" || input == "about:blank") {
            return input
        }
        val lower = input.lowercase()
        if (category == SearchCategory.WEB) {
            if (lower.startsWith("https://") || lower.startsWith("http://")) {
                return input
            }
            val hasSpaces = input.contains(" ")
            val hasDot = input.contains(".")
            if (!hasSpaces && hasDot && input.length >= 4) {
                return "https://$input"
            }
        }
        return currentSearchEngine.buildQueryUrl(input, category)
    }

    fun openNewTab(url: String = "openv://home", isPrivate: Boolean = false) {
        tabManager.openNewTab(url, isPrivate)
        _loadUrlEvent.value = url

        // Rotate VPN session on new browser session if configured
        vpnManager.triggerNewSessionRotation()
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
        tabManager.updateActiveTab { it.copy(isDesktopMode = !current) }
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
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
                WebStorage.getInstance().deleteAllData()

                val dummyWebView = WebView(context)
                dummyWebView.clearCache(true)
                dummyWebView.destroy()
                TrackerBlocker.resetCounter()
            } catch (_: Exception) {}

            withContext(Dispatchers.IO) {
                historyDao.clearAllHistory()
            }
            onComplete()
        }
    }

    // Settings actions
    fun setSearchEngine(engine: SearchEngine) {
        viewModelScope.launch { prefsRepo.setSearchEngine(engine) }
    }

    fun setDnsProvider(provider: DnsProvider) {
        viewModelScope.launch { dnsRepository.setProvider(provider) }
    }

    fun setCustomDnsUrl(url: String) {
        viewModelScope.launch { dnsRepository.setCustomDohUrl(url) }
    }

    fun setSafeBrowsing(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setSafeBrowsing(enabled) }
    }

    fun setBlockThirdPartyCookies(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setBlockThirdPartyCookies(enabled) }
    }

    fun setDesktopMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDesktopMode(enabled) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDarkTheme(enabled) }
    }

    // OSINT Real Actions
    fun executeDoHQuery(domain: String, type: String = "A") {
        viewModelScope.launch {
            val config = dnsConfiguration.value
            val result = dohClient.resolve(
                domain = domain,
                recordType = type,
                provider = config.provider,
                customDohUrl = config.customDohUrl
            )
            _dohQueryResult.value = result
        }
    }

    fun inspectUrlHeaders(url: String) {
        viewModelScope.launch {
            val result = securityClient.inspectHttpHeadersAndRedirects(url)
            _httpInspectionResult.value = result.getOrNull()
        }
    }

    fun checkUsernames(username: String) {
        viewModelScope.launch {
            val list = securityClient.checkUsernameProfiles(username)
            _socialCheckResults.value = list
        }
    }

    fun lookupRdap(target: String) {
        viewModelScope.launch {
            val result = securityClient.lookupRdap(target)
            _rdapResult.value = result.getOrNull()
        }
    }

    fun inspectExif(uri: Uri) {
        viewModelScope.launch {
            val result = exifManager.extractExif(uri)
            _exifResult.value = result.getOrNull()
        }
    }

    fun stripExif(uri: Uri) {
        viewModelScope.launch {
            val result = exifManager.stripMetadata(uri)
            _sanitizedImageFile.value = result.getOrNull()
        }
    }

    fun saveOsintReport(title: String, category: String, target: String, rawData: String, summary: String) {
        viewModelScope.launch(Dispatchers.IO) {
            osintReportDao.insertReport(
                OsintReportEntity(
                    title = title,
                    category = category,
                    target = target,
                    rawData = rawData,
                    summary = summary
                )
            )
        }
    }

    fun deleteOsintReport(report: OsintReportEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            osintReportDao.deleteReport(report)
        }
    }

    fun clearAllOsintReports() {
        viewModelScope.launch(Dispatchers.IO) {
            osintReportDao.clearAllReports()
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

            vpnManager.refreshPublicIp()
        }
    }
}
