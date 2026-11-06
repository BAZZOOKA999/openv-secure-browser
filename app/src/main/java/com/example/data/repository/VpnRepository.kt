package com.example.data.repository

import com.example.data.model.VpnConnectionState
import com.example.data.model.VpnProfile
import com.example.data.model.VpnProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface VpnRepository {
    val connectionState: Flow<VpnConnectionState>
    val activeProfile: Flow<VpnProfile?>
    val availableProfiles: Flow<List<VpnProfile>>
    val killSwitchEnabled: Flow<Boolean>

    fun selectProfile(profileId: String)
    fun addProfile(profile: VpnProfile)
    fun removeProfile(profileId: String)
    fun toggleKillSwitch(enabled: Boolean)
    fun getDiagnosticsInfo(): String
}

class VpnRepositoryImpl : VpnRepository {

    // Honest initial state: NOT_CONFIGURED
    private val _connectionState = MutableStateFlow(VpnConnectionState.NOT_CONFIGURED)
    override val connectionState: Flow<VpnConnectionState> = _connectionState.asStateFlow()

    private val _availableProfiles = MutableStateFlow<List<VpnProfile>>(
        listOf(
            VpnProfile(
                id = "profile_br_east",
                name = "Servidor Brasil (São Paulo)",
                serverHost = "vpn-br.openv.internal",
                serverPort = 51820,
                protocol = VpnProtocol.WIREGUARD,
                countryCode = "BR",
                isConfigured = false
            ),
            VpnProfile(
                id = "profile_us_east",
                name = "Servidor EUA (Virgínia)",
                serverHost = "vpn-us.openv.internal",
                serverPort = 51820,
                protocol = VpnProtocol.WIREGUARD,
                countryCode = "US",
                isConfigured = false
            ),
            VpnProfile(
                id = "profile_ch_zurich",
                name = "Servidor Suíça (Zurique - Privacidade)",
                serverHost = "vpn-ch.openv.internal",
                serverPort = 51820,
                protocol = VpnProtocol.WIREGUARD,
                countryCode = "CH",
                isConfigured = false
            )
        )
    )
    override val availableProfiles: Flow<List<VpnProfile>> = _availableProfiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<VpnProfile?>(null)
    override val activeProfile: Flow<VpnProfile?> = _activeProfile.asStateFlow()

    private val _killSwitchEnabled = MutableStateFlow(false)
    override val killSwitchEnabled: Flow<Boolean> = _killSwitchEnabled.asStateFlow()

    override fun selectProfile(profileId: String) {
        val profile = _availableProfiles.value.find { it.id == profileId }
        _activeProfile.value = profile
    }

    override fun addProfile(profile: VpnProfile) {
        _availableProfiles.value = _availableProfiles.value + profile
    }

    override fun removeProfile(profileId: String) {
        _availableProfiles.value = _availableProfiles.value.filterNot { it.id == profileId }
        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = null
        }
    }

    override fun toggleKillSwitch(enabled: Boolean) {
        _killSwitchEnabled.value = enabled
    }

    override fun getDiagnosticsInfo(): String {
        return "VPN Engine: Desconectada (Módulo nativo em preparação para Etapa 2). Nenhuma interface TUN ativa."
    }
}

class VpnSessionManager(
    private val vpnRepository: VpnRepository
) {
    private var currentSessionId: String = UUID.randomUUID().toString()
    private var sessionStartTime: Long = System.currentTimeMillis()

    fun rotateSession() {
        currentSessionId = UUID.randomUUID().toString()
        sessionStartTime = System.currentTimeMillis()
    }

    fun getSessionDetails(): Map<String, Any> {
        return mapOf(
            "sessionId" to currentSessionId,
            "sessionStart" to sessionStartTime,
            "isRotatedOnNewTab" to true
        )
    }
}
