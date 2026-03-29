package com.aikeyboard.core.di

import com.aikeyboard.feature.clipboard.data.repository.ClipboardRepositoryImpl
import com.aikeyboard.feature.clipboard.domain.repository.ClipboardRepository
import com.aikeyboard.feature.clipboard.domain.usecase.GetClipboardItemsUseCase
import com.aikeyboard.feature.clipboard.domain.usecase.SaveToCredentialsUseCase
import com.aikeyboard.feature.credentials.data.repository.CredentialRepositoryImpl
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import com.aikeyboard.feature.credentials.domain.usecase.GetCredentialsUseCase
import com.aikeyboard.feature.keyboard.data.repository.SuggestionRepositoryImpl
import com.aikeyboard.feature.keyboard.domain.repository.SuggestionRepository
import com.aikeyboard.feature.keyboard.domain.usecase.GetSuggestionsUseCase
import com.aikeyboard.feature.keyboard.ui.KeyboardViewModel
import com.aikeyboard.feature.settings.data.repository.SettingsRepositoryImpl
import com.aikeyboard.feature.settings.domain.repository.SettingsRepository

/**
 * Manual dependency injection container.
 *
 * All repositories are singletons — created once and reused.
 * To migrate to Hilt, simply annotate each impl with @Singleton
 * and replace [provideKeyboardViewModel] with a @HiltViewModel.
 *
 * Dependency graph:
 *   SettingsRepository  (singleton)
 *   SuggestionRepository (singleton)
 *   ClipboardRepository  (singleton)
 *   CredentialRepository (singleton)
 *       └─ SaveToCredentialsUseCase (needs both clipboard + credential repos)
 *   KeyboardViewModel (new instance per IME session)
 */
object AppModule {

    // ── Repositories (lazy singletons) ────────────────────────────────────────

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl()
    }

    val suggestionRepository: SuggestionRepository by lazy {
        SuggestionRepositoryImpl()
    }

    val clipboardRepository: ClipboardRepository by lazy {
        ClipboardRepositoryImpl()
    }

    val credentialRepository: CredentialRepository by lazy {
        CredentialRepositoryImpl()
    }

    // ── Use Cases (lazy, depend on repositories above) ────────────────────────

    private val getSuggestionsUseCase by lazy {
        GetSuggestionsUseCase(suggestionRepository)
    }

    private val getClipboardItemsUseCase by lazy {
        GetClipboardItemsUseCase(clipboardRepository)
    }

    private val getCredentialsUseCase by lazy {
        GetCredentialsUseCase(credentialRepository)
    }

    private val saveToCredentialsUseCase by lazy {
        SaveToCredentialsUseCase(clipboardRepository, credentialRepository)
    }

    // ── ViewModel factory ─────────────────────────────────────────────────────

    /** Call this once from [com.aikeyboard.PixelProIME] to get the ViewModel. */
    fun provideKeyboardViewModel(): KeyboardViewModel = KeyboardViewModel(
        getSuggestionsUseCase    = getSuggestionsUseCase,
        getClipboardItemsUseCase = getClipboardItemsUseCase,
        getCredentialsUseCase    = getCredentialsUseCase,
        saveToCredentialsUseCase = saveToCredentialsUseCase,
    )
}
