package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.data.local.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Base64
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiLiveApi"

/**
 * Gemini 2.5 Flash API client for speech-to-text
 */
class GeminiLiveApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    suspend fun transcribe(audioFile: File, language: String): ApiTranscriptionResult = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists()) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.NO_AUDIO, "Audio file not found", null)
            }
            if (audioFile.length() == 0L) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.NO_AUDIO, "Audio file is empty", null)
            }

            val apiKey = preferencesManager.getGeminiApiKey().trim()
            if (apiKey.isEmpty()) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.AUTH, "No API key configured", "Enter your Gemini API key")
            }

            Log.d(TAG, "Transcribing: ${audioFile.name}, size: ${audioFile.length()}, lang: $language")

            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.getEncoder().encodeToString(audioBytes)
            val mimeType = getMimeType(audioFile)

            val languageInstruction = if (language == "bn") {
                "Transcribe the following audio in Bengali. Output only the transcribed text."
            } else {
                "Transcribe the following audio in English. Output only the transcribed text."
            }

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

            val request = Request.Builder()
                .url("${ApiConstants.GEMINI_ENDPOINT}/models/${ApiConstants.GEMINI_MODEL}:generateContent?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Content-Type", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response: ${response.code}")

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val json = JSONObject(bodyString)
                    val candidates = json.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val text = candidates.getJSONObject(0)
                            .optJSONObject("content")
                            ?.optJSONArray("parts")
                            ?.getJSONObject(0)
                            ?.optString("text", "")
                        if (!text.isNullOrBlank()) {
                            return@withContext ApiTranscriptionResult.success(text.trim())
                        }
                    }
                    ApiTranscriptionResult.error(ApiErrorType.EMPTY_RESULT, "No speech detected", null)
                } else {
                    parseError(response.code, bodyString)
                }
            }
        } catch (e: SocketTimeoutException) {
            ApiTranscriptionResult.error(ApiErrorType.TIMEOUT, "Request timed out", e.message)
        } catch (e: UnknownHostException) {
            ApiTranscriptionResult.error(ApiErrorType.NETWORK, "No internet connection", e.message)
        } catch (e: Exception) {
            ApiTranscriptionResult.error(ApiErrorType.UNKNOWN, "Transcription failed", e.message)
        }
    }

    fun isConfigured(): Boolean = preferencesManager.hasGeminiApiKey()

    fun getSupportedLanguages(): List<String> = listOf("en", "bn")

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

    private fun parseError(code: Int, body: String?): ApiTranscriptionResult {
        val parsedMessage = try {
            body?.let {
                JSONObject(it).optJSONObject("error")?.optString("message", null)
            } ?: "API Error: $code"
        } catch (e: Exception) { "API Error: $code" }

        return when (code) {
            400 -> ApiTranscriptionResult.error(ApiErrorType.INVALID_KEY, "Invalid request", parsedMessage)
            401, 403 -> ApiTranscriptionResult.error(ApiErrorType.INVALID_KEY, "Invalid API key", "Get key from aistudio.google.com/apikey")
            429 -> ApiTranscriptionResult.error(ApiErrorType.RATE_LIMIT, "Rate limit exceeded", "Wait and try again")
            500, 502, 503, 504 -> ApiTranscriptionResult.error(ApiErrorType.SERVER, "Server error", "Try again later")
            else -> ApiTranscriptionResult.error(ApiErrorType.UNKNOWN, parsedMessage, "HTTP $code")
        }
    }

    companion object {
        @Volatile
        private var instance: GeminiLiveApi? = null

        fun getInstance(context: Context): GeminiLiveApi {
            return instance ?: synchronized(this) {
                instance ?: GeminiLiveApi(context.applicationContext).also { instance = it }
            }
        }
    }
}
