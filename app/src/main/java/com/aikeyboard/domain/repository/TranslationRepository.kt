package com.aikeyboard.domain.repository

import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranslationResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for translation operations
 * 
 * This interface defines the contract for translation implementations.
 * The domain layer depends on this abstraction, not concrete implementations.
 */
interface TranslationRepository {
    
    /**
     * Translate text from source language to target language
     * 
     * @param text The text to translate
     * @param sourceLanguage The source language
     * @param targetLanguage The target language
     * @return Flow of TranslationResult to track progress
     */
    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Flow<TranslationResult>
    
    /**
     * Check if translation service is available
     */
    suspend fun isServiceAvailable(): Boolean
    
    /**
     * Get supported language pairs for translation
     */
    fun getSupportedLanguagePairs(): List<Pair<Language, Language>>
}
