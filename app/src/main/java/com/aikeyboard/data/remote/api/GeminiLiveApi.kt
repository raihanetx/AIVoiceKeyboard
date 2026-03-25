package com.aikeyboard.data.remote.api

import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.core.security.Secrets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.Base64
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiLiveApi"

/**
 * Gemini 2.5 Flash Live API client for speech-to-text transcription
 * 
 * Uses Gemini's native audio input capability for real-time transcription.
 * Free tier: 15 RPM, 1 million tokens/day
 * Get API key from: https://aistudio.google.com/apikey
 */
class GeminiLiveApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    /**
     * Transcribe audio file to text using Gemini 2.5 Flash
     * 
     * @param audioFile The audio file to transcribe
     * @param language The language code (e.g., "en", "bn")
     * @return Result containing the transcribed text or an error
     */
    suspend fun transcribe(
        audioFile: File,
        language: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty"))
            }

            // Check API key
            val apiKey = Secrets.GEMINI_API_KEY
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("Gemini API key not configured. Get free key from https://aistudio.google.com/apikey"))
            }

            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)
            val mimeType = getMimeType(audioFile)

            Log.d(TAG, "Transcribing with Gemini: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")

            val languageInstruction = if (language == "bn") {
                "Transcribe the following audio in Bengali language. Output only the transcribed text, nothing else."
            } else {
                "Transcribe the following audio in English language. Output only the transcribed text, nothing else."
            }

            // Build JSON request for Gemini
            val jsonBody = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("inlineData", JSONObject().apply {
                                    put("mimeType", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                            put(JSONObject().apply {
                                put("text", languageInstruction)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
                    put("maxOutputTokens", 1024)
                })
            }

            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val url = "${ApiConstants.GEMINI_ENDPOINT}/models/${ApiConstants.GEMINI_MODEL}:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                
                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val jsonResponse = JSONObject(bodyString)
                    val candidates = jsonResponse.optJSONArray("candidates")
                    
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0)
                            .optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        
                        if (parts != null && parts.length() > 0) {
                            val text = parts.getJSONObject(0).optString("text", "")
                            if (text.isNotBlank()) {
                                Log.d(TAG, "Gemini transcription success: ${text.take(50)}...")
                                return@withContext Result.success(text.trim())
                            }
                        }
                    }
                    
                    Result.failure(Exception("Empty transcription result"))
                } else {
                    val errorMessage = parseError(response.code, bodyString)
                    Log.e(TAG, "API Error: ${response.code} - $errorMessage")
                    Result.failure(Exception(errorMessage))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(Exception("Transcription failed: ${e.message}"))
        }
    }

    /**
     * Check if the API is configured and available
     */
    fun isConfigured(): Boolean {
        return Secrets.isGeminiApiKeyConfigured()
    }

    /**
     * Get supported languages
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("en", "bn")
    }

    /**
     * Get MIME type for audio file
     */
    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "3gp" -> "audio/3gpp"
            "mp4", "m4a" -> "audio/mp4"
            "webm" -> "audio/webm"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "ogg" -> "audio/ogg"
            else -> "audio/3gpp"
        }
    }

    /**
     * Parse error response
     */
    private fun parseError(code: Int, body: String?): String {
        if (body.isNullOrBlank()) return "API Error: $code"
        
        return try {
            val json = JSONObject(body)
            val error = json.optJSONObject("error")
            error?.optString("message", "API Error: $code") ?: "API Error: $code"
        } catch (e: Exception) {
            "API Error: $code"
        }
    }

    companion object {
        val instance by lazy { GeminiLiveApi() }
    }
}
