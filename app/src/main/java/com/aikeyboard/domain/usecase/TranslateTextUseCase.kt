package com.aikeyboard.domain.usecase

import android.util.Log
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranslationResult
import com.aikeyboard.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val TAG = "TranslateTextUseCase"

/**
 * Use case for translating text between languages
 * 
 * This use case encapsulates the business logic for text translation,
 * including input validation and language handling.
 */
class TranslateTextUseCase(
    private val translationRepository: TranslationRepository
) {
    /**
     * Execute translation
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return Flow of TranslationResult
     */
    suspend operator fun invoke(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Flow<TranslationResult> = flow {
        emit(TranslationResult.Loading)
        
        // Validate input
        if (text.isBlank()) {
            Log.e(TAG, "Text cannot be empty")
            emit(TranslationResult.Error("Text cannot be empty"))
            return@flow
        }
        
        if (text.length > AppConstants.MAX_TEXT_LENGTH_FOR_TRANSLATION) {
            Log.e(TAG, "Text too long: ${text.length} characters")
            emit(TranslationResult.Error("Text too long (max ${AppConstants.MAX_TEXT_LENGTH_FOR_TRANSLATION} characters)"))
            return@flow
        }
        
        // Validate languages are different
        if (sourceLanguage == targetLanguage) {
            Log.w(TAG, "Source and target languages are the same")
            emit(TranslationResult.Success(
                translatedText = text,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage
            ))
            return@flow
        }
        
        // Delegate to repository
        translationRepository.translate(text, sourceLanguage, targetLanguage).collect { result ->
            emit(result)
        }
    }

    /**
     * Translate with language codes
     * 
     * @param text The text to translate
     * @param sourceLangCode Source language code (e.g., "en", "bn")
     * @param targetLangCode Target language code (e.g., "en", "bn")
     * @return Flow of TranslationResult
     */
    suspend fun translateWithCodes(
        text: String,
        sourceLangCode: String,
        targetLangCode: String
    ): Flow<TranslationResult> {
        val sourceLanguage = Language.fromCode(sourceLangCode)
        val targetLanguage = Language.fromCode(targetLangCode)
        return invoke(text, sourceLanguage, targetLanguage)
    }

    /**
     * Quick translate from English to Bengali
     */
    suspend fun translateToBengali(text: String): Flow<TranslationResult> {
        return invoke(text, Language.ENGLISH, Language.BENGALI)
    }

    /**
     * Quick translate from Bengali to English
     */
    suspend fun translateToEnglish(text: String): Flow<TranslationResult> {
        return invoke(text, Language.BENGALI, Language.ENGLISH)
    }

    /**
     * Check if translation service is available
     */
    suspend fun isAvailable(): Boolean {
        return translationRepository.isServiceAvailable()
    }
}
