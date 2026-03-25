package com.aikeyboard.translation

import com.aikeyboard.AiKeyboardApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ZAiClient private constructor() {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build()

    suspend fun translate(text: String, sourceLang: String, targetLang: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val srcName = if (sourceLang == "en") "English" else "Bengali"
            val tgtName = if (targetLang == "en") "English" else "Bengali"
            val jsonBody = JSONObject().apply {
                put("model", "glm-4.7-flash")
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", "You are a translator. Only output the translation."))
                    put(JSONObject().put("role", "user").put("content", "Translate from $srcName to $tgtName: $text"))
                })
                put("max_tokens", 2048)
                put("temperature", 0.3)
            }
            val request = Request.Builder()
                .url("${AiKeyboardApp.ZAI_ENDPOINT}/chat/completions")
                .addHeader("Authorization", "Bearer ${AiKeyboardApp.ZAI_API_KEY}")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType())).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val content = JSONObject(response.body?.string()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                Result.success(content.trim())
            } else Result.failure(Exception("API Error: ${response.code}"))
        } catch (e: Exception) { Result.failure(e) }
    }
    companion object { val instance by lazy { ZAiClient() } }
}
