package com.aikeyboard.feature.settings.data.repository

import com.aikeyboard.feature.settings.domain.model.SettingsState
import com.aikeyboard.feature.settings.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory settings store.
 * Swap this for a DataStore-backed implementation without touching
 * any other layer — the interface stays the same.
 */
class SettingsRepositoryImpl : SettingsRepository {

    private val _settings = MutableStateFlow(SettingsState())

    override fun getSettings(): Flow<SettingsState> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: SettingsState) {
        _settings.value = settings
    }
}
