package com.aikeyboard.feature.credentials.domain.usecase

import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full credentials list. */
class GetCredentialsUseCase(private val repo: CredentialRepository) {
    operator fun invoke(): Flow<List<CredentialEntry>> = repo.getCredentials()
}

/** Adds a new credential entry. */
class AddCredentialUseCase(private val repo: CredentialRepository) {
    suspend operator fun invoke(entry: CredentialEntry) = repo.addCredential(entry)
}

/** Removes a credential by id. */
class RemoveCredentialUseCase(private val repo: CredentialRepository) {
    suspend operator fun invoke(id: String) = repo.removeCredential(id)
}
