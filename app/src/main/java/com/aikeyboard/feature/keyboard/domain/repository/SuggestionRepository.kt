package com.aikeyboard.feature.keyboard.domain.repository

/** Contract for the word-prediction engine. */
interface SuggestionRepository {
    /**
     * Given the current typed buffer, returns up to 3 suggested words.
     * Returns an empty list when there is nothing to suggest.
     */
    fun getSuggestions(bufferText: String): List<String>
}
