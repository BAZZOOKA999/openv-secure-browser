package com.example.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.data.local.AppDatabase
import com.example.data.local.VpnProfileEntity
import com.example.data.model.PublicIpInfo
import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnProfile
import com.example.data.model.VpnProtocol
import com.example.network.NetworkSecurityClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VpnManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object { const val ENGINE_AVAILABLE = false }

    private val db = AppDatabase.getInstance(context)
    private val vpnDao = db.vpnProfileDao()
    private val securityClient = NetworkSecurityClient()

    val connectionState: StateFlow<VpnConnectionState> = OpenVVpnService.vpnStatus

    val connectedServer: StateFlow<String?> = OpenVVpnService.connectedServer

    private val _isNavigationBlocked = MutableStateFlow(false)
    val isNavigationBlocked: StateFlow<Boolean> = _isNavigationBlocked.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(false)
    val killSwitchEnabled: StateFlow<Boolean> = _killSwitchEnabled.asStateFlow()

    private val _autoRotatePerSession = MutableStateFlow(false)
    val autoRotatePerSession: StateFlow<Boolean> = _autoRotatePerSession.asStateFlow()

    private val _currentPublicIp = MutableStateFlow<PublicIpInfo?>(null)
    val currentPublicIp: StateFlow<PublicIpInfo?> = _currentPublicIp.asStateFlow()

    private val _activeProfile = MutableStateFlow<VpnProfile?>(null)
    val activeProfile: StateFlow<VpnProfile?> = _activeProfile.asStateFlow()

    val savedProfiles: StateFlow<List<VpnProfile>> = vpnDao.getAllProfiles().map { list ->
        if (list.isEmpty()) {
            getDefaultProfiles()
        } else {
            list.map {
                VpnProfile(
                    id = it.id,
                    name = it.name,
                    serverHost = it.serverHost,
                    serverPort = it.serverPort,
                    protocol = try { VpnProtocol.valueOf(it.protocol) } catch (_: Exception) { VpnProtocol.WIREGUARD },
                    countryCode = it.countryCode,
                    username = it.username,
                    dnsServer = it.dnsServer,
                    isConfigured = true
                )
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), getDefaultProfiles())

    init {
        // Pre-populate default profiles if empty
        scope.launch(Dispatchers.IO) {
            val defaults = getDefaultProfiles()
            defaults.forEach { p ->
                vpnDao.insertProfile(
                    VpnProfileEntity(
                        id = p.id,
                        name = p.name,
                        serverHost = p.serverHost,
                        serverPort = p.serverPort,
                        protocol = p.protocol.name,
                        countryCode = p.countryCode,
                        username = p.username,
                        dnsServer = p.dnsServer
                    )
                )
            }
            if (_activeProfile.value == null) {
                _activeProfile.value = defaults.firstOrNull()
            }
        }
        refreshPublicIp()
    }

    private fun getDefaultProfiles(): List<VpnProfile> {
        return listOf(
            VpnProfile(
                id = "prof_brazil",
                name = "Servidor Brasil (São Paulo)",
                serverHost = "br.vpn.openv.net",
                serverPort = 51820,
                protocol = VpnProtocol.WIREGUARD,
                countryCode = "BR",
                dnsServer = "1.1.1.1"
            ),
            VpnProfile(
                id = "prof_switzerland",
                name = "Servidor Suíça (Zurique - Privacidade)",
                serverHost = "ch.vpn.openv.net",
                serverPort = 51820,
                protocol = VpnProtocol.WIREGUARD,
                countryCode = "CH",
                dnsServer = "9.9.9.9"
            ),
            VpnProfile(
                id = "prof_us",
                name = "Servidor EUA (Virgínia)",
                serverHost = "us.vpn.openv.net",
                serverPort = 51820,
                protocol = VpnProtocol.OPENVPN_UDP,
                countryCode = "US",
                dnsServer = "8.8.8.8"
            )
        )
    }

    fun selectProfile(profile: VpnProfile) {
        _activeProfile.value = profile
    }

    fun toggleKillSwitch(enabled: Boolean) {
        _killSwitchEnabled.value = enabled && ENGINE_AVAILABLE
    }

    fun toggleAutoRotate(enabled: Boolean) {
        _autoRotatePerSession.value = enabled && ENGINE_AVAILABLE
    }

    fun addProfile(profile: VpnProfile) {
        scope.launch(Dispatchers.IO) {
            vpnDao.insertProfile(
                VpnProfileEntity(
                    id = profile.id,
                    name = profile.name,
                    serverHost = profile.serverHost,
                    serverPort = profile.serverPort,
                    protocol = profile.protocol.name,
                    countryCode = profile.countryCode,
                    username = profile.username,
                    dnsServer = profile.dnsServer
                )
            )
            _activeProfile.value = profile
        }
    }

    fun deleteProfile(profileId: String) {
        scope.launch(Dispatchers.IO) {
            vpnDao.deleteById(profileId)
            if (_activeProfile.value?.id == profileId) {
                _activeProfile.value = savedProfiles.value.firstOrNull { it.id != profileId }
            }
        }
    }

    /**
     * Checks if Android VpnService permission needs to be requested.
     * Returns Intent if user consent is needed, or null if already granted.
     */
    fun checkVpnPermission(activity: Activity): Intent? {
        return VpnService.prepare(activity)
    }

    fun startVpn(profile: VpnProfile? = null) {
        if (!ENGINE_AVAILABLE) {
            android.widget.Toast.makeText(context, "VPN indisponível: o motor de túnel ainda não está implementado. Use um aplicativo VPN externo.", android.widget.Toast.LENGTH_LONG).show()
            return
        }
        val target = profile ?: _activeProfile.value ?: return
        val intent = Intent(context, OpenVVpnService::class.java).apply {
            action = OpenVVpnService.ACTION_CONNECT
            putExtra(OpenVVpnService.EXTRA_SERVER_HOST, target.serverHost)
            putExtra(OpenVVpnService.EXTRA_SERVER_PORT, target.serverPort)
            putExtra(OpenVVpnService.EXTRA_DNS, target.dnsServer)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopVpn() {
        val intent = Intent(context, OpenVVpnService::class.java).apply {
            action = OpenVVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun refreshPublicIp() {
        scope.launch {
            val result = securityClient.fetchPublicIpInfo()
            _currentPublicIp.value = result.getOrNull()
        }
    }

    /**
     * Full per-session rotation procedure:
     * 1. Temporarily blocks new navigation.
     * 2. Selects next server profile.
     * 3. Re-establishes VPN if active.
     * 4. Verifies public IP and connectivity.
     * 5. Releases navigation upon verification (or keeps blocked if Kill Switch on failure).
     */
    fun triggerNewSessionRotation(onComplete: (Boolean) -> Unit = {}) {
        if (!ENGINE_AVAILABLE || !_autoRotatePerSession.value) {
            onComplete(true)
            return
        }

        scope.launch {
            _isNavigationBlocked.value = true

            val profiles = savedProfiles.value
            if (profiles.isNotEmpty()) {
                val current = _activeProfile.value
                val currentIndex = profiles.indexOfFirst { it.id == current?.id }
                val nextIndex = (currentIndex + 1) % profiles.size
                val nextProfile = profiles[nextIndex]
                _activeProfile.value = nextProfile

                if (connectionState.value == VpnConnectionState.CONNECTED) {
                    stopVpn()
                    withContext(Dispatchers.IO) { Thread.sleep(400) }
                    startVpn(nextProfile)
                }
            }

            // Verify connectivity & IP
            val ipResult = securityClient.fetchPublicIpInfo()
            _currentPublicIp.value = ipResult.getOrNull()

            val success = ipResult.isSuccess
            if (success || !_killSwitchEnabled.value) {
                _isNavigationBlocked.value = false
                onComplete(true)
            } else {
                // Keep blocked under strict protection if check fails
                _isNavigationBlocked.value = true
                onComplete(false)
            }
        }
    }

    /**
     * Helper to open third-party VPN apps (WireGuard, OpenVPN) if user prefers external tunnels
     */
    fun openExternalVpnApp(packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
