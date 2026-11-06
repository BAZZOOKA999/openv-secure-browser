package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.DnsProvider
import com.example.data.model.SearchEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openv_settings")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val SAFE_BROWSING = booleanPreferencesKey("safe_browsing")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val DNS_PROVIDER = stringPreferencesKey("dns_provider")
        val CUSTOM_DNS_URL = stringPreferencesKey("custom_dns_url")
        val KILL_SWITCH_PREVIEW = booleanPreferencesKey("kill_switch_preview")
        val CLEAR_ON_EXIT = booleanPreferencesKey("clear_on_exit")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val searchEngineFlow: Flow<SearchEngine> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.name
        try {
            SearchEngine.valueOf(name)
        } catch (_: Exception) {
            SearchEngine.DUCKDUCKGO
        }
    }

    suspend fun setSearchEngine(engine: SearchEngine) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SEARCH_ENGINE] = engine.name
        }
    }

    val safeBrowsingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SAFE_BROWSING] ?: true
    }

    suspend fun setSafeBrowsing(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SAFE_BROWSING] = enabled
        }
    }

    val blockThirdPartyCookiesFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.BLOCK_THIRD_PARTY_COOKIES] ?: true
    }

    suspend fun setBlockThirdPartyCookies(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.BLOCK_THIRD_PARTY_COOKIES] = enabled
        }
    }

    val desktopModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DESKTOP_MODE_DEFAULT] ?: false
    }

    suspend fun setDesktopMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DESKTOP_MODE_DEFAULT] = enabled
        }
    }

    val dnsProviderFlow: Flow<DnsProvider> = context.dataStore.data.map { prefs ->
        val name = prefs[PreferencesKeys.DNS_PROVIDER] ?: DnsProvider.CLOUDFLARE.name
        try {
            DnsProvider.valueOf(name)
        } catch (_: Exception) {
            DnsProvider.CLOUDFLARE
        }
    }

    suspend fun setDnsProvider(provider: DnsProvider) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DNS_PROVIDER] = provider.name
        }
    }

    val customDnsUrlFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_DNS_URL] ?: ""
    }

    suspend fun setCustomDnsUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CUSTOM_DNS_URL] = url
        }
    }

    val killSwitchFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.KILL_SWITCH_PREVIEW] ?: false
    }

    suspend fun setKillSwitch(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.KILL_SWITCH_PREVIEW] = enabled
        }
    }

    val clearOnExitFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CLEAR_ON_EXIT] ?: false
    }

    suspend fun setClearOnExit(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CLEAR_ON_EXIT] = enabled
        }
    }

    val darkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.DARK_THEME] ?: true
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DARK_THEME] = enabled
        }
    }
}
