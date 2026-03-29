package com.aikeyboard.feature.keyboard.data.repository

import com.aikeyboard.feature.keyboard.data.source.Dictionary
import com.aikeyboard.feature.keyboard.domain.repository.SuggestionRepository

/** Implements word prediction using the static [Dictionary]. */
class SuggestionRepositoryImpl : SuggestionRepository {

    override fun getSuggestions(bufferText: String): List<String> {
        if (bufferText.trim().isEmpty()) return emptyList()

        val isTrailingSpace = bufferText.endsWith(' ') || bufferText.endsWith('\n')
        val words           = bufferText.trimEnd().split(Regex("[\\s\n]+"))
        val currentWord     = if (isTrailingSpace) "" else words.lastOrNull().orEmpty()
        val prevWord        = when {
            words.size > 1                          -> words[words.size - 2].lowercase()
            words.size == 1 && currentWord.isEmpty() -> words[0].lowercase()
            else                                    -> ""
        }

        return if (currentWord.isNotEmpty()) {
            Dictionary.words
                .filter { it.startsWith(currentWord.lowercase()) && it != currentWord.lowercase() }
                .take(3)
                .ifEmpty { Dictionary.generic }
        } else {
            Dictionary.nextWordMap[prevWord] ?: Dictionary.generic
        }
    }
}
