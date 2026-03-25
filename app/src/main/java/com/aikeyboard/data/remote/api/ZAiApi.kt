package com.aikeyboard.data.remote.api

import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.core.security.Secrets
import com.aikeyboard.data.remote.dto.TranslationRequest
import com.aikeyboard.data.remote.dto.TranslationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "ZAiApi"

/**
 * Z-AI API client for text translation
 * 
 * Uses GLM-4.7-Flash model for accurate translations.
 */
class ZAiApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.TRANSLATION_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.TRANSLATION_TIMEOUT, TimeUnit.SECONDS)
        .build()

    /**
     * Translate text from source language to target language
     * 
     * @param text The text to translate
     * @param sourceLang Source language code (e.g., "en", "bn")
     * @param targetLang Target language code (e.g., "en", "bn")
     * @return Result containing the translated text or an error
     */
    suspend fun translate(
        text: String,
        sourceLang: String,
        targetLang: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate input
            if (text.isBlank()) {
                return@withContext Result.failure(Exception("Text cannot be empty"))
            }
            if (text.length > 5000) {
                return@withContext Result.failure(Exception("Text too long (max 5000 characters)"))
            }

            // Check API key
            val apiKey = Secrets.ZAI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Z-AI API key not configured"))
            }

            // Create request
            val request = TranslationRequest.create(
                model = ApiConstants.ZAI_MODEL,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                text = text
            )

            val httpRequest = Request.Builder()
                .url("${ApiConstants.ZAI_ENDPOINT}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(request.toJson().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "Translating: ${sourceLang} -> ${targetLang}, text: ${text.take(30)}...")

            client.newCall(httpRequest).execute().use { response ->
                val bodyString = response.body?.string()
                
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val translationResponse = TranslationResponse.fromJson(bodyString)
                    
                    if (translationResponse.isValid) {
                        Log.d(TAG, "Translation success: ${translationResponse.translatedText.take(50)}...")
                        Result.success(translationResponse.translatedText)
                    } else {
                        Result.failure(Exception("Empty translation"))
                    }
                } else {
                    val errorMessage = "API Error: ${response.code}"
                    Log.e(TAG, "$errorMessage - $bodyString")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "No internet connection", e)
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: Exception) {
            Log.e(TAG, "Translation failed", e)
            Result.failure(Exception("Translation failed: ${e.message}"))
        }
    }

    /**
     * Check if the API is configured and available
     */
    fun isConfigured(): Boolean {
        return Secrets.isZaiApiKeyConfigured()
    }

    companion object {
        val instance by lazy { ZAiApi() }
    }
}
