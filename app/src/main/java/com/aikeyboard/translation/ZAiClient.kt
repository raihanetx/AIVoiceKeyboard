package com.aikeyboard.translation

import android.util.Log
import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "ZAiClient"

object ZAiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
            
            val srcName = if (sourceLang == "en") "English" else "Bengali"
            val tgtName = if (targetLang == "en") "English" else "Bengali"
            
            val jsonBody = JSONObject().apply {
                put("model", "glm-4.7-flash")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a translator. Translate the given text accurately. Only output the translation, nothing else - no explanations, no notes.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", "Translate from $srcName to $tgtName: $text")
                    })
                })
                put("max_tokens", 2048)
                put("temperature", 0.3)
            }

            val request = Request.Builder()
                .url("${AiKeyboardApp.ZAI_ENDPOINT}/chat/completions")
                .addHeader("Authorization", "Bearer ${AiKeyboardApp.ZAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
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
                        val choices = jsonResponse.optJSONArray("choices")
                        
                        if (choices == null || choices.length() == 0) {
                            return@withContext Result.failure(Exception("No translation returned"))
                        }
                        
                        // Try to get content from message
                        val messageObj = choices.getJSONObject(0).optJSONObject("message")
                        
                        // GLM-4.7-flash may return content in "content" or "reasoning_content"
                        var content = messageObj?.optString("content", "") ?: ""
                        
                        // If content is empty, try reasoning_content
                        if (content.isBlank()) {
                            content = messageObj?.optString("reasoning_content", "") ?: ""
                        }
                            
                        if (content.isBlank()) {
                            return@withContext Result.failure(Exception("Empty translation"))
                        }
                        
                        Result.success(content.trim())
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
            Log.e(TAG, "Translation failed", e)
            Result.failure(Exception("Translation failed: ${e.message}"))
        }
    }
}
