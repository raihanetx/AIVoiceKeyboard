package com.aikeyboard.voice

import android.util.Base64
import android.util.Log
import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "GeminiVoiceClient"

class GeminiVoiceClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(audioFile: File, language: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Validate file
            if (!audioFile.exists()) {
                return@withContext Result.failure(Exception("Audio file does not exist"))
            }
            if (audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty"))
            }
            
            val audioBytes = audioFile.readBytes()
            val audioBase64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            
            // Determine mime type based on file extension
            val mimeType = when (audioFile.extension.lowercase()) {
                "3gp" -> "audio/3gpp"
                "mp4", "m4a" -> "audio/mp4"
                "webm" -> "audio/webm"
                "wav" -> "audio/wav"
                "mp3" -> "audio/mpeg"
                "aac" -> "audio/aac"
                "ogg" -> "audio/ogg"
                "flac" -> "audio/flac"
                else -> "audio/3gpp" // Default to 3gpp for 3GP files
            }
            
            Log.d(TAG, "Transcribing audio: ${audioFile.name}, mimeType: $mimeType, size: ${audioFile.length()}")
            
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Transcribe this audio to ${if(language=="bn") "Bengali" else "English"}. Only output the transcribed text, nothing else."))
                            put(JSONObject().put("inline_data", JSONObject().put("mime_type", mimeType).put("data", audioBase64)))
                        })
                    })
                })
            }
            
            val url = "${AiKeyboardApp.GEMINI_ENDPOINT}/${AiKeyboardApp.GEMINI_AUDIO_MODEL}:generateContent?key=${AiKeyboardApp.GEMINI_API_KEY}"
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            // Use 'use' to ensure response body is closed
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (bodyString.isNullOrBlank()) {
                        return@withContext Result.failure(Exception("Empty response body"))
                    }
                    
                    try {
                        val jsonResponse = JSONObject(bodyString)
                        val candidates = jsonResponse.optJSONArray("candidates")
                        if (candidates == null || candidates.length() == 0) {
                            return@withContext Result.failure(Exception("No transcription returned"))
                        }
                        
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        if (content == null) {
                            return@withContext Result.failure(Exception("Invalid response format"))
                        }
                        
                        val parts = content.optJSONArray("parts")
                        if (parts == null || parts.length() == 0) {
                            return@withContext Result.failure(Exception("No transcription parts"))
                        }
                        
                        val text = parts.getJSONObject(0).optString("text", "")
                        if (text.isBlank()) {
                            return@withContext Result.failure(Exception("Empty transcription"))
                        }
                        
                        Result.success(text.trim())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        Result.failure(Exception("Failed to parse response: ${e.message}"))
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: ${response.code} - $errorBody")
                    Result.failure(Exception("API Error: ${response.code}"))
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Request timed out", e)
            Result.failure(Exception("Request timed out. Please try again."))
        } catch (e: UnknownHostException) {
            Log.e(TAG, "No internet connection", e)
            Result.failure(Exception("No internet connection. Please check your network."))
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            Result.failure(Exception("Transcription failed: ${e.message}"))
        }
    }
    
    companion object { 
        val instance by lazy { GeminiVoiceClient() } 
    }
}
