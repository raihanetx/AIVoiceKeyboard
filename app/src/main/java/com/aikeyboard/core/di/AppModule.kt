package com.aikeyboard.core.di

import android.content.Context
import com.aikeyboard.feature.clipboard.data.repository.ClipboardRepositoryImpl
import com.aikeyboard.feature.clipboard.domain.repository.ClipboardRepository
import com.aikeyboard.feature.clipboard.domain.usecase.GetClipboardItemsUseCase
import com.aikeyboard.feature.clipboard.domain.usecase.SaveToCredentialsUseCase
import com.aikeyboard.feature.credentials.data.repository.CredentialRepositoryImpl
import com.aikeyboard.feature.credentials.domain.repository.CredentialRepository
import com.aikeyboard.feature.credentials.domain.usecase.AddCredentialUseCase
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

    // ── Context (set from IME) ────────────────────────────────────────────────
    
    private var appContext: Context? = null
    
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ── Repositories (lazy singletons) ────────────────────────────────────────

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl()
    }

    val suggestionRepository: SuggestionRepository by lazy {
        SuggestionRepositoryImpl()
    }

    val clipboardRepository: ClipboardRepository by lazy {
        ClipboardRepositoryImpl(appContext!!)
    }

    val credentialRepository: CredentialRepository by lazy {
        CredentialRepositoryImpl(appContext!!)
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

    private val addCredentialUseCase by lazy {
        AddCredentialUseCase(credentialRepository)
    }

    private val saveToCredentialsUseCase by lazy {
        SaveToCredentialsUseCase(clipboardRepository, credentialRepository)
    }

    // ── ViewModel factory ─────────────────────────────────────────────────────

    /** Call this once from [com.aikeyboard.PixelProIME] to get the ViewModel. */
    fun provideKeyboardViewModel(context: Context): KeyboardViewModel {
        // Initialize app context if not set
        if (appContext == null) {
            init(context)
        }
        
        return KeyboardViewModel(
            getSuggestionsUseCase    = getSuggestionsUseCase,
            getClipboardItemsUseCase = getClipboardItemsUseCase,
            getCredentialsUseCase    = getCredentialsUseCase,
            addCredentialUseCase     = addCredentialUseCase,
            saveToCredentialsUseCase = saveToCredentialsUseCase,
            context                  = context,
        )
    }
}
