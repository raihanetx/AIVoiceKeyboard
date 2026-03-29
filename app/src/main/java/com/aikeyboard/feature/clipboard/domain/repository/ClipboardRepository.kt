package com.aikeyboard.feature.clipboard.domain.repository

import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import kotlinx.coroutines.flow.Flow

interface ClipboardRepository {
    fun getClipboardItems(): Flow<List<ClipboardEntry>>
    suspend fun removeItem(id: String)
}
