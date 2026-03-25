package com.aikeyboard.di

import android.content.Context
import com.aikeyboard.core.util.AudioRecorder
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.api.GroqWhisperApi
import com.aikeyboard.data.remote.api.ZAiApi
import com.aikeyboard.data.repository.TranscriptionRepositoryImpl
import com.aikeyboard.data.repository.TranslationRepositoryImpl
import com.aikeyboard.domain.repository.TranscriptionRepository
import com.aikeyboard.domain.repository.TranslationRepository
import com.aikeyboard.domain.usecase.GetAvailableLanguagesUseCase
import com.aikeyboard.domain.usecase.TranscribeAudioUseCase
import com.aikeyboard.domain.usecase.TranslateTextUseCase

/**
 * Manual Dependency Injection Module
 * 
 * Provides dependencies for the application without using a DI framework.
 * This is a simple service locator pattern for managing dependencies.
 */
object AppModule {

    // ==================== API Clients ====================

    private var _groqWhisperApi: GroqWhisperApi? = null
    val groqWhisperApi: GroqWhisperApi
        get() = _groqWhisperApi ?: GroqWhisperApi.instance.also { _groqWhisperApi = it }

    private var _zAiApi: ZAiApi? = null
    val zAiApi: ZAiApi
        get() = _zAiApi ?: ZAiApi.instance.also { _zAiApi = it }

    // ==================== Repositories ====================

    private var _transcriptionRepository: TranscriptionRepository? = null
    val transcriptionRepository: TranscriptionRepository
        get() = _transcriptionRepository ?: TranscriptionRepositoryImpl(groqWhisperApi)
            .also { _transcriptionRepository = it }

    private var _translationRepository: TranslationRepository? = null
    val translationRepository: TranslationRepository
        get() = _translationRepository ?: TranslationRepositoryImpl(zAiApi)
            .also { _translationRepository = it }

    // ==================== Preferences ====================

    private var _preferencesManager: PreferencesManager? = null

    fun getPreferencesManager(context: Context): PreferencesManager {
        return _preferencesManager ?: PreferencesManager.getInstance(context)
            .also { _preferencesManager = it }
    }

    // ==================== Use Cases ====================

    private var _transcribeAudioUseCase: TranscribeAudioUseCase? = null

    fun getTranscribeAudioUseCase(context: Context): TranscribeAudioUseCase {
        return _transcribeAudioUseCase ?: TranscribeAudioUseCase(
            transcriptionRepository = transcriptionRepository,
            audioRecorder = AudioRecorder(context.applicationContext)
        ).also { _transcribeAudioUseCase = it }
    }

    private var _translateTextUseCase: TranslateTextUseCase? = null
    val translateTextUseCase: TranslateTextUseCase
        get() = _translateTextUseCase ?: TranslateTextUseCase(translationRepository)
            .also { _translateTextUseCase = it }

    private var _getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase? = null
    val getAvailableLanguagesUseCase: GetAvailableLanguagesUseCase
        get() = _getAvailableLanguagesUseCase ?: GetAvailableLanguagesUseCase(
            transcriptionRepository = transcriptionRepository,
            translationRepository = translationRepository
        ).also { _getAvailableLanguagesUseCase = it }

    // ==================== Utility Methods ====================

    /**
     * Clear all cached instances
     * Useful for testing or when re-initialization is needed
     */
    fun clearAll() {
        _groqWhisperApi = null
        _zAiApi = null
        _transcriptionRepository = null
        _translationRepository = null
        _preferencesManager = null
        _transcribeAudioUseCase = null
        _translateTextUseCase = null
        _getAvailableLanguagesUseCase = null
    }
}
