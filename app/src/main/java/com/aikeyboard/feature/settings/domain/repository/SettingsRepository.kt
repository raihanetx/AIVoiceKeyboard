package com.aikeyboard.feature.settings.domain.repository

import com.aikeyboard.feature.settings.domain.model.SettingsState
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettings(): Flow<SettingsState>
    suspend fun updateSettings(settings: SettingsState)
}
