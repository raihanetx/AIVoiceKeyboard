package com.aikeyboard.domain.usecase

import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.repository.TranscriptionRepository
import com.aikeyboard.domain.repository.TranslationRepository

/**
 * Use case for getting available languages
 * 
 * This use case provides language selection functionality for both
 * transcription and translation features.
 */
class GetAvailableLanguagesUseCase(
    private val transcriptionRepository: TranscriptionRepository,
    private val translationRepository: TranslationRepository
) {
    /**
     * Get all available languages
     */
    fun getAllLanguages(): List<Language> {
        return Language.getAll()
    }

    /**
     * Get languages supported for transcription
     */
    fun getTranscriptionLanguages(): List<String> {
        return transcriptionRepository.getSupportedLanguages()
    }

    /**
     * Get language pairs supported for translation
     */
    fun getTranslationLanguagePairs(): List<Pair<Language, Language>> {
        return translationRepository.getSupportedLanguagePairs()
    }

    /**
     * Get default language
     */
    fun getDefaultLanguage(): Language {
        return Language.ENGLISH
    }

    /**
     * Get opposite language for translation
     * 
     * If current language is English, returns Bengali and vice versa
     */
    fun getOppositeLanguage(currentLanguage: Language): Language {
        return when (currentLanguage) {
            Language.ENGLISH -> Language.BENGALI
            Language.BENGALI -> Language.ENGLISH
        }
    }

    /**
     * Get language from code
     */
    fun getLanguageFromCode(code: String): Language {
        return Language.fromCode(code)
    }

    /**
     * Check if language pair is supported for translation
     */
    fun isLanguagePairSupported(source: Language, target: Language): Boolean {
        return translationRepository.getSupportedLanguagePairs().any { pair ->
            pair.first == source && pair.second == target
        }
    }
}
