package com.aikeyboard.feature.credentials.domain.repository

import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import kotlinx.coroutines.flow.Flow

interface CredentialRepository {
    fun getCredentials(): Flow<List<CredentialEntry>>
    suspend fun addCredential(entry: CredentialEntry)
    suspend fun removeCredential(id: String)
}
