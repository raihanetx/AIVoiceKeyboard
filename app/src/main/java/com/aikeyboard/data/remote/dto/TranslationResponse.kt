package com.aikeyboard.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

/**
 * Data Transfer Object for translation API response
 * 
 * Matches the response format from Z-AI Chat Completions API
 */
data class TranslationResponse(
    val translatedText: String,
    val model: String? = null,
    val usage: Usage? = null,
    val finishReason: String? = null
) {
    /**
     * Check if response is valid (has non-empty text)
     */
    val isValid: Boolean
        get() = translatedText.isNotBlank()

    /**
     * Token usage information
     */
    data class Usage(
        val promptTokens: Int,
        val completionTokens: Int,
        val totalTokens: Int
    )

    companion object {
        /**
         * Parse TranslationResponse from JSON string
         */
        fun fromJson(jsonString: String): TranslationResponse {
            return try {
                val json = JSONObject(jsonString)
                val choices = json.optJSONArray("choices")
                
                val content = if (choices != null && choices.length() > 0) {
                    val message = choices.getJSONObject(0).optJSONObject("message")
                    // Try "content" first, then "reasoning_content" for GLM models
                    message?.optString("content", "")?.takeIf { it.isNotBlank() }
                        ?: message?.optString("reasoning_content", "") ?: ""
                } else {
                    ""
                }

                val usage = parseUsage(json.optJSONObject("usage"))

                TranslationResponse(
                    translatedText = content.trim(),
                    model = json.optString("model", null),
                    usage = usage,
                    finishReason = choices?.getJSONObject(0)?.optString("finish_reason", null)
                )
            } catch (e: Exception) {
                TranslationResponse(translatedText = "")
            }
        }

        private fun parseUsage(usageObj: JSONObject?): Usage? {
            if (usageObj == null) return null
            
            return Usage(
                promptTokens = usageObj.optInt("prompt_tokens", 0),
                completionTokens = usageObj.optInt("completion_tokens", 0),
                totalTokens = usageObj.optInt("total_tokens", 0)
            )
        }
    }

    /**
     * Convert to JSON string
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("translated_text", translatedText)
            model?.let { put("model", it) }
            usage?.let { 
                put("usage", JSONObject().apply {
                    put("prompt_tokens", it.promptTokens)
                    put("completion_tokens", it.completionTokens)
                    put("total_tokens", it.totalTokens)
                })
            }
            finishReason?.let { put("finish_reason", it) }
        }.toString()
    }
}

/**
 * DTO for translation API request
 */
data class TranslationRequest(
    val model: String,
    val messages: List<Message>,
    val maxTokens: Int = 2048,
    val temperature: Double = 0.3
) {
    data class Message(
        val role: String,
        val content: String
    )

    /**
     * Convert to JSON string for API request
     */
    fun toJson(): String {
        return JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                messages.forEach { message ->
                    put(JSONObject().apply {
                        put("role", message.role)
                        put("content", message.content)
                    })
                }
            })
            put("max_tokens", maxTokens)
            put("temperature", temperature)
        }.toString()
    }

    companion object {
        /**
         * Create a translation request
         */
        fun create(
            model: String,
            sourceLanguage: String,
            targetLanguage: String,
            text: String
        ): TranslationRequest {
            val srcName = if (sourceLanguage == "en") "English" else "Bengali"
            val tgtName = if (targetLanguage == "en") "English" else "Bengali"

            return TranslationRequest(
                model = model,
                messages = listOf(
                    Message(
                        role = "system",
                        content = "You are a translator. Translate the given text accurately. Only output the translation, nothing else - no explanations, no notes."
                    ),
                    Message(
                        role = "user",
                        content = "Translate from $srcName to $tgtName: $text"
                    )
                )
            )
        }
    }
}
