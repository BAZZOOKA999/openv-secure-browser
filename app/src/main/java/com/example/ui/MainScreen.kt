package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.browser.AddressBar
import com.example.ui.browser.BookmarksAndHistoryDialog
import com.example.ui.browser.BrowserViewModel
import com.example.ui.browser.OpenVHomeScreen
import com.example.ui.browser.OpenVWebView
import com.example.ui.browser.TabSwitcherDialog
import com.example.ui.osint.OsintScreen
import com.example.ui.protection.ProtectionScreen
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.OpenVAlertRed
import com.example.ui.theme.OpenVBackground
import com.example.ui.theme.OpenVBlue
import com.example.ui.theme.OpenVBorder
import com.example.ui.theme.OpenVShieldGreen
import com.example.ui.theme.OpenVSurface
import com.example.ui.theme.OpenVSurfaceVariant
import com.example.ui.theme.OpenVTextPrimary
import com.example.ui.theme.OpenVTextSecondary

sealed class NavSection(val title: String, val icon: ImageVector, val tag: String) {
    object Browser : NavSection("Navegador", Icons.Default.Language, "nav_browser_tab")
    object Search : NavSection("Pesquisa", Icons.Default.Search, "nav_search_tab")
    object Shield : NavSection("Proteção", Icons.Default.Shield, "nav_shield_tab")
    object Osint : NavSection("OSINT", Icons.Default.TravelExplore, "nav_osint_tab")
    object Settings : NavSection("Configurações", Icons.Default.Settings, "nav_settings_tab")
}

@Composable
fun MainScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    var currentSectionIndex by remember { mutableIntStateOf(0) }

    val activeTab by viewModel.activeTab.collectAsState()
    val tabs by viewModel.tabs.collectAsState()
    val searchEngine by viewModel.searchEngine.collectAsState()
    val safeBrowsingEnabled by viewModel.safeBrowsingEnabled.collectAsState()
    val blockThirdPartyCookies by viewModel.blockThirdPartyCookies.collectAsState()
    val desktopModeDefault by viewModel.desktopModeDefault.collectAsState()
    val darkThemeEnabled by viewModel.darkThemeEnabled.collectAsState()

    val vpnState by viewModel.vpnState.collectAsState()
    val dnsConfig by viewModel.dnsConfiguration.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()
    val diagnosticState by viewModel.diagnosticState.collectAsState()
    val loadUrlEvent by viewModel.loadUrlEvent.collectAsState()

    var showTabSwitcher by remember { mutableStateOf(false) }
    var showBookmarksAndHistory by remember { mutableStateOf(false) }

    val isCurrentUrlBookmarked = remember(activeTab?.url, bookmarks) {
        val currentUrl = activeTab?.url ?: ""
        bookmarks.any { it.url == currentUrl }
    }

    // Handle Android system back gesture / button
    BackHandler(enabled = showTabSwitcher || showBookmarksAndHistory || currentSectionIndex != 0 || (activeTab != null && activeTab?.url != "openv://home")) {
        when {
            showTabSwitcher -> showTabSwitcher = false
            showBookmarksAndHistory -> showBookmarksAndHistory = false
            currentSectionIndex != 0 -> currentSectionIndex = 0
            activeTab?.canGoBack == true -> {
                // Let webview handle back or navigate back
                viewModel.tabManager.updateActiveTab { it.copy(canGoBack = false) }
                // In actual webview back, handled internally or triggers goHome
                if (activeTab?.url != "openv://home") {
                    viewModel.goHome()
                }
            }
            activeTab?.url != "openv://home" -> {
                viewModel.goHome()
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = OpenVSurface,
                contentColor = OpenVBlue,
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    NavSection.Browser,
                    NavSection.Search,
                    NavSection.Shield,
                    NavSection.Osint,
                    NavSection.Settings
                )

                items.forEachIndexed { index, section ->
                    val isSelected = currentSectionIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentSectionIndex = index },
                        icon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = section.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (section == NavSection.Shield) OpenVShieldGreen else OpenVBlue,
                            selectedTextColor = if (section == NavSection.Shield) OpenVShieldGreen else OpenVBlue,
                            unselectedIconColor = OpenVTextSecondary,
                            unselectedTextColor = OpenVTextSecondary,
                            indicatorColor = OpenVSurfaceVariant
                        ),
                        modifier = Modifier.testTag(section.tag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(OpenVBackground)
        ) {
            when (currentSectionIndex) {
                0 -> {
                    // Navegador Tab
                    Column(modifier = Modifier.fillMaxSize()) {
                        AddressBar(
                            tab = activeTab,
                            tabCount = tabs.size,
                            isBookmarked = isCurrentUrlBookmarked,
                            onNavigate = { queryOrUrl ->
                                viewModel.navigateTo(queryOrUrl)
                            },
                            onReload = {
                                activeTab?.url?.let { viewModel.navigateTo(it) }
                            },
                            onHome = { viewModel.goHome() },
                            onOpenTabSwitcher = { showTabSwitcher = true },
                            onOpenBookmarksAndHistory = { showBookmarksAndHistory = true },
                            onToggleBookmark = {
                                activeTab?.let {
                                    viewModel.toggleBookmark(it.title, it.url)
                                }
                            },
                            onToggleDesktopMode = {
                                viewModel.toggleDesktopMode()
                            },
                            onOpenNewPrivateTab = {
                                viewModel.openNewTab(isPrivate = true)
                            },
                            onClearData = {
                                viewModel.clearBrowsingData(context)
                            }
                        )

                        // Main Content: Home Screen or Live WebView or Error Screen
                        val currentTab = activeTab
                        if (currentTab == null || currentTab.url == "openv://home" || currentTab.url == "about:blank") {
                            OpenVHomeScreen(
                                searchEngine = searchEngine,
                                bookmarks = bookmarks,
                                isPrivateTab = currentTab?.isPrivate == true,
                                onNavigate = { urlOrQuery ->
                                    viewModel.navigateTo(urlOrQuery)
                                },
                                onSelectEngine = { engine ->
                                    viewModel.setSearchEngine(engine)
                                }
                            )
                        } else if (currentTab.errorMessage != null) {
                            // Error State Card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(OpenVBackground)
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = OpenVSurface),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, OpenVAlertRed)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = OpenVAlertRed,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Erro no Carregamento",
                                            color = OpenVTextPrimary,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentTab.errorMessage ?: "Não foi possível carregar a página solicitada.",
                                            color = OpenVTextSecondary,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.navigateTo(currentTab.url)
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = OpenVBlue),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Tentar Novamente", fontSize = 12.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.goHome() },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = OpenVSurfaceVariant),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Página Inicial", color = OpenVTextPrimary, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Live Android WebView
                            OpenVWebView(
                                tab = currentTab,
                                blockThirdPartyCookies = blockThirdPartyCookies,
                                safeBrowsingEnabled = safeBrowsingEnabled,
                                onTitleChanged = { title ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(title = title) }
                                },
                                onProgressChanged = { progress ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(progress = progress) }
                                },
                                onLoadingStateChanged = { loading ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(isLoading = loading) }
                                },
                                onNavigationStateChanged = { canBack, canForward ->
                                    viewModel.tabManager.updateTab(currentTab.id) {
                                        it.copy(canGoBack = canBack, canGoForward = canForward)
                                    }
                                },
                                onUrlChanged = { newUrl ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(url = newUrl) }
                                    viewModel.recordVisit(currentTab.title, newUrl, currentTab.isPrivate)
                                },
                                onSslStatusChanged = { isSsl ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(sslCertificateValid = isSsl) }
                                },
                                onErrorOccurred = { errorMsg ->
                                    viewModel.tabManager.updateTab(currentTab.id) { it.copy(errorMessage = errorMsg, isLoading = false) }
                                },
                                onSaveState = { bundle ->
                                    viewModel.tabManager.saveTabState(currentTab.id, bundle)
                                },
                                savedState = viewModel.tabManager.getTabSavedState(currentTab.id),
                                externalLoadUrl = loadUrlEvent,
                                onExternalUrlConsumed = {
                                    viewModel.clearLoadUrlEvent()
                                }
                            )
                        }
                    }
                }

                1 -> {
                    // Pesquisa Tab
                    SearchScreen(
                        searchEngine = searchEngine,
                        history = history,
                        onSelectEngine = { engine -> viewModel.setSearchEngine(engine) },
                        onExecuteSearch = { query ->
                            currentSectionIndex = 0
                            viewModel.navigateTo(query)
                        }
                    )
                }

                2 -> {
                    // Proteção Tab
                    ProtectionScreen(
                        vpnState = vpnState,
                        dnsConfig = dnsConfig,
                        safeBrowsingEnabled = safeBrowsingEnabled,
                        blockThirdPartyCookies = blockThirdPartyCookies,
                        diagnosticState = diagnosticState,
                        onToggleSafeBrowsing = { viewModel.setSafeBrowsing(it) },
                        onToggleBlockThirdPartyCookies = { viewModel.setBlockThirdPartyCookies(it) },
                        onRunDiagnostics = { viewModel.runConnectivityDiagnostics() },
                        onClearBrowsingData = { viewModel.clearBrowsingData(context) }
                    )
                }

                3 -> {
                    // OSINT Tab
                    OsintScreen(
                        onLaunchQueryInBrowser = { url ->
                            currentSectionIndex = 0
                            viewModel.navigateTo(url)
                        }
                    )
                }

                4 -> {
                    // Configurações Tab
                    SettingsScreen(
                        currentSearchEngine = searchEngine,
                        dnsConfig = dnsConfig,
                        safeBrowsingEnabled = safeBrowsingEnabled,
                        blockThirdPartyCookies = blockThirdPartyCookies,
                        desktopModeDefault = desktopModeDefault,
                        darkThemeEnabled = darkThemeEnabled,
                        onSelectSearchEngine = { viewModel.setSearchEngine(it) },
                        onSelectDnsProvider = { viewModel.setDnsProvider(it) },
                        onSetCustomDnsUrl = { viewModel.setCustomDnsUrl(it) },
                        onToggleSafeBrowsing = { viewModel.setSafeBrowsing(it) },
                        onToggleBlockThirdPartyCookies = { viewModel.setBlockThirdPartyCookies(it) },
                        onToggleDesktopMode = { viewModel.toggleDesktopMode() },
                        onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                        onClearAllData = { viewModel.clearBrowsingData(context) }
                    )
                }
            }

            // Dialogs
            if (showTabSwitcher) {
                TabSwitcherDialog(
                    tabs = tabs,
                    activeTabId = activeTab?.id ?: "",
                    onSelectTab = { tabId -> viewModel.selectTab(tabId) },
                    onCloseTab = { tabId -> viewModel.closeTab(tabId) },
                    onNewTab = { viewModel.openNewTab() },
                    onNewPrivateTab = { viewModel.openNewTab(isPrivate = true) },
                    onCloseAllTabs = { viewModel.closeAllTabs() },
                    onDismiss = { showTabSwitcher = false }
                )
            }

            if (showBookmarksAndHistory) {
                BookmarksAndHistoryDialog(
                    bookmarks = bookmarks,
                    history = history,
                    onSelectUrl = { url ->
                        currentSectionIndex = 0
                        viewModel.navigateTo(url)
                    },
                    onDeleteBookmark = { bookmark -> viewModel.deleteBookmark(bookmark) },
                    onClearHistory = { viewModel.clearAllHistory() },
                    onDismiss = { showBookmarksAndHistory = false }
                )
            }
        }
    }
}
