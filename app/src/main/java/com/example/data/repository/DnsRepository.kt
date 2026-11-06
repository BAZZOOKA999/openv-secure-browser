package com.example.data.repository

import com.example.data.local.UserPreferencesRepository
import com.example.data.model.DnsConfiguration
import com.example.data.model.DnsProtectionState
import com.example.data.model.DnsProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

interface DnsRepository {
    val configuration: Flow<DnsConfiguration>
    suspend fun setProvider(provider: DnsProvider)
    suspend fun setCustomDohUrl(url: String)
}

class DnsRepositoryImpl(
    private val preferencesRepository: UserPreferencesRepository
) : DnsRepository {

    override val configuration: Flow<DnsConfiguration> = combine(
        preferencesRepository.dnsProviderFlow,
        preferencesRepository.customDnsUrlFlow
    ) { provider, customUrl ->
        DnsConfiguration(
            provider = provider,
            customDohUrl = customUrl,
            // Honest state in Stage 1: Selected and persisted, ready for stage 2 integration
            state = DnsProtectionState.NOT_APPLIED
        )
    }

    override suspend fun setProvider(provider: DnsProvider) {
        preferencesRepository.setDnsProvider(provider)
    }

    override suspend fun setCustomDohUrl(url: String) {
        preferencesRepository.setCustomDnsUrl(url)
    }
}
