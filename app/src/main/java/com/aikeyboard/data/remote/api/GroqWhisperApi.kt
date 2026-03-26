package com.aikeyboard.data.remote.api

import android.content.Context
import android.util.Log
import com.aikeyboard.core.constants.ApiConstants
import com.aikeyboard.data.local.PreferencesManager
import com.aikeyboard.data.remote.dto.TranscriptionError
import com.aikeyboard.data.remote.dto.TranscriptionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "GroqWhisperApi"

/**
 * Groq Whisper API client for speech-to-text transcription
 */
class GroqWhisperApi(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(ApiConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(ApiConstants.READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(ApiConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "AIVoiceKeyboard/3.9.0 (Android)")
                .addHeader("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private val preferencesManager: PreferencesManager by lazy { PreferencesManager.getInstance(context) }

    private fun getApiKey(): String = preferencesManager.getGroqApiKey().trim()

    suspend fun transcribe(audioFile: File, language: String): ApiTranscriptionResult = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists()) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.NO_AUDIO, "Audio file not found", null)
            }
            if (audioFile.length() == 0L) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.NO_AUDIO, "Audio file is empty", null)
            }

            val apiKey = getApiKey()
            if (apiKey.isEmpty()) {
                return@withContext ApiTranscriptionResult.error(ApiErrorType.AUTH, "No API key configured", "Enter your Groq API key")
            }

            Log.d(TAG, "Transcribing: ${audioFile.name}, size: ${audioFile.length()}, lang: $language")

            val audioBytes = audioFile.readBytes()
            val mimeType = getMimeType(audioFile)

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("model", ApiConstants.GROQ_WHISPER_MODEL)
                .addFormDataPart("language", if (language == "bn") "bn" else "en")
                .addFormDataPart("response_format", "json")
                .addFormDataPart("temperature", "0.0")
                .addFormDataPart("file", audioFile.name, audioBytes.toRequestBody(mimeType.toMediaType()))
                .build()

            val request = Request.Builder()
                .url("${ApiConstants.GROQ_ENDPOINT}/audio/transcriptions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                Log.d(TAG, "Response: ${response.code}")

                if (response.isSuccessful && !bodyString.isNullOrBlank()) {
                    val transcriptionResponse = TranscriptionResponse.fromJson(bodyString)
                    if (transcriptionResponse.isValid) {
                        ApiTranscriptionResult.success(transcriptionResponse.text.trim())
                    } else {
                        ApiTranscriptionResult.error(ApiErrorType.EMPTY_RESULT, "No speech detected", null)
                    }
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

    fun isConfigured(): Boolean = preferencesManager.hasGroqApiKey()

    fun getSupportedLanguages(): List<String> = listOf("en", "bn")

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "3gp" -> "audio/3gpp"
            "mp4", "m4a" -> "audio/mp4"
            "webm" -> "audio/webm"
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            else -> "audio/3gpp"
        }
    }

    private fun parseError(code: Int, body: String?): ApiTranscriptionResult {
        val parsedMessage = try {
            body?.let { TranscriptionError.fromJson(it).message } ?: "API Error: $code"
        } catch (e: Exception) { "API Error: $code" }

        return when (code) {
            401 -> ApiTranscriptionResult.error(ApiErrorType.INVALID_KEY, "Invalid API key", "Check your key at console.groq.com/keys")
            403 -> ApiTranscriptionResult.error(ApiErrorType.AUTH, "Access denied", parsedMessage)
            429 -> ApiTranscriptionResult.error(ApiErrorType.RATE_LIMIT, "Rate limit exceeded", "Wait and try again")
            500, 502, 503, 504 -> ApiTranscriptionResult.error(ApiErrorType.SERVER, "Server error", "Try again later")
            else -> ApiTranscriptionResult.error(ApiErrorType.UNKNOWN, parsedMessage, "HTTP $code")
        }
    }

    companion object {
        @Volatile
        private var instance: GroqWhisperApi? = null

        fun getInstance(context: Context): GroqWhisperApi {
            return instance ?: synchronized(this) {
                instance ?: GroqWhisperApi(context.applicationContext).also { instance = it }
            }
        }
    }
}
