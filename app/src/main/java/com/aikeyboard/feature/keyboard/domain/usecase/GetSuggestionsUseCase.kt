package com.aikeyboard.feature.keyboard.domain.usecase

import com.aikeyboard.feature.keyboard.domain.repository.SuggestionRepository

/**
 * Returns up to 3 word predictions for the current buffer text.
 * Keeps the ViewModel free of suggestion logic.
 */
class GetSuggestionsUseCase(private val repository: SuggestionRepository) {
    operator fun invoke(bufferText: String): List<String> =
        repository.getSuggestions(bufferText)
}
