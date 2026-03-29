package com.aikeyboard.feature.clipboard.domain.usecase

import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.clipboard.domain.repository.ClipboardRepository
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow

/** Streams all clipboard items. */
class GetClipboardItemsUseCase(private val repo: ClipboardRepository) {
    operator fun invoke(): Flow<List<ClipboardEntry>> = repo.getClipboardItems()
}

/** Moves a clipboard entry into the credentials store. */
class SaveToCredentialsUseCase(
    private val clipRepo : ClipboardRepository,
    private val credRepo : CredentialRepository,
) {
    suspend operator fun invoke(clip: ClipboardEntry) {
        clipRepo.removeItem(clip.id)
        credRepo.addCredential(
            CredentialEntry(id = clip.id, text = clip.text, label = "Saved Item", iconType = CredIconType.LOCK)
        )
    }
}
