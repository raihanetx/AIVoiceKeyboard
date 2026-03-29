package com.aikeyboard.feature.clipboard.data.repository

import com.aikeyboard.core.base.BaseListRepository
import com.aikeyboard.feature.clipboard.domain.model.ClipIconType
import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.clipboard.domain.repository.ClipboardRepository
import kotlinx.coroutines.flow.Flow

class ClipboardRepositoryImpl : BaseListRepository<ClipboardEntry>(
    initialItems = listOf(
        ClipboardEntry("c1", "Sounds good, let me know.", "10 mins ago", ClipIconType.HISTORY),
        ClipboardEntry("c2", "https://google.com",        "1 hour ago",  ClipIconType.LINK),
    )
), ClipboardRepository {

    override fun getClipboardItems(): Flow<List<ClipboardEntry>> = items

    override suspend fun removeItem(id: String) = removeItem { it.id == id }
}
