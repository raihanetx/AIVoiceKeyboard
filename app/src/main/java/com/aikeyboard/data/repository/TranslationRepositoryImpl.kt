package com.aikeyboard.data.repository

import com.aikeyboard.data.remote.api.ZAiApi
import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranslationResult
import com.aikeyboard.domain.repository.TranslationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Implementation of TranslationRepository using Z-AI API
 */
class TranslationRepositoryImpl(
    private val zAiApi: ZAiApi = ZAiApi.instance
) : TranslationRepository {

    override suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): Flow<TranslationResult> = flow {
        emit(TranslationResult.Loading)

        val result = zAiApi.translate(
            text = text,
            sourceLang = sourceLanguage.code,
            targetLang = targetLanguage.code
        )

        result.fold(
            onSuccess = { translatedText ->
                emit(TranslationResult.Success(
                    translatedText = translatedText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage
                ))
            },
            onFailure = { error ->
                emit(TranslationResult.Error(
                    message = error.message ?: "Translation failed"
                ))
            }
        )
    }

    override suspend fun isServiceAvailable(): Boolean {
        return zAiApi.isConfigured()
    }

    override fun getSupportedLanguagePairs(): List<Pair<Language, Language>> {
        return Language.getLanguagePairs()
    }
}
