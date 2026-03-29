package com.aikeyboard.feature.credentials.data.repository

import com.aikeyboard.core.base.BaseListRepository
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow

class CredentialRepositoryImpl : BaseListRepository<CredentialEntry>(
    initialItems = listOf(
        CredentialEntry("cr1", "contact@design.io", "Saved Pin", CredIconType.LOCK),
        CredentialEntry("cr2", "123-456-7890",      "Saved Pin", CredIconType.PHONE),
    )
), CredentialRepository {

    override fun getCredentials(): Flow<List<CredentialEntry>> = items

    override suspend fun addCredential(entry: CredentialEntry) = addItem(entry, prepend = true)

    override suspend fun removeCredential(id: String) = removeItem { it.id == id }
}
