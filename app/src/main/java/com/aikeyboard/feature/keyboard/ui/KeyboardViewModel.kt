package com.aikeyboard.feature.keyboard.ui

import androidx.lifecycle.viewModelScope
import com.aikeyboard.core.base.BaseViewModel
import com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry
import com.aikeyboard.feature.clipboard.domain.usecase.GetClipboardItemsUseCase
import com.aikeyboard.feature.clipboard.domain.usecase.SaveToCredentialsUseCase
import com.aikeyboard.feature.credentials.domain.usecase.GetCredentialsUseCase
import com.aikeyboard.feature.keyboard.domain.model.*
import com.aikeyboard.feature.keyboard.domain.usecase.GetSuggestionsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Single ViewModel for the entire keyboard surface.
 * All UI state lives here; composables only read [state] and call functions.
 */
class KeyboardViewModel(
    private val getSuggestionsUseCase    : GetSuggestionsUseCase,
    private val getClipboardItemsUseCase : GetClipboardItemsUseCase,
    private val getCredentialsUseCase    : GetCredentialsUseCase,
    private val saveToCredentialsUseCase : SaveToCredentialsUseCase,
) : BaseViewModel<KeyboardState>(KeyboardState()) {

    // ── Clipboard & Credentials (streamed from their own features) ────────────

    val clipboardItems: StateFlow<List<com.aikeyboard.feature.clipboard.domain.model.ClipboardEntry>> =
        getClipboardItemsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val credentials: StateFlow<List<com.aikeyboard.feature.credentials.domain.model.CredentialEntry>> =
        getCredentialsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Typing ────────────────────────────────────────────────────────────────

    /** Insert one character or string into the buffer. */
    fun insert(text: String) = setState {
        val newBuffer = bufferText + text
        val newCaps   = if (language == KeyboardLanguage.EN && isCaps && text.length == 1) false else isCaps
        copy(bufferText = newBuffer, isCaps = newCaps, suggestions = getSuggestions(newBuffer))
    }

    /** Delete the last character in the buffer. */
    fun backspace() = setState {
        val newBuffer = bufferText.dropLast(1).takeIf { bufferText.isNotEmpty() } ?: ""
        copy(bufferText = newBuffer, suggestions = getSuggestions(newBuffer))
    }

    /** Replace the current partial word with the tapped prediction. */
    fun usePrediction(word: String) = setState {
        val newBuffer = if (!bufferText.endsWith(' ') && !bufferText.endsWith('\n') && bufferText.isNotEmpty()) {
            val trimmed = Regex("[a-zA-Z]+$").find(bufferText)?.let { m ->
                bufferText.dropLast(m.value.length)
            } ?: bufferText
            trimmed + word + " "
        } else {
            bufferText + word + " "
        }
        copy(bufferText = newBuffer, suggestions = getSuggestions(newBuffer))
    }

    private fun getSuggestions(buffer: String) = getSuggestionsUseCase(buffer)

    // ── Layout controls ───────────────────────────────────────────────────────

    fun toggleCaps()    = setState { copy(isCaps = !isCaps) }
    fun toggleMode()    = setState { copy(mode = if (mode == KeyboardMode.NUM) KeyboardMode.ALPHA else KeyboardMode.NUM) }
    fun swapLanguage()  = setState { copy(language = if (language == KeyboardLanguage.EN) KeyboardLanguage.BN else KeyboardLanguage.EN, mode = KeyboardMode.ALPHA, isCaps = false) }

    // ── Overlay navigation ────────────────────────────────────────────────────

    fun showOverlay(type: OverlayType) = setState { copy(activeOverlay = type) }
    fun hideOverlay()                  = setState { copy(activeOverlay = OverlayType.NONE) }

    // ── Voice input ───────────────────────────────────────────────────────────

    fun startVoice()         = setState { copy(isVoiceActive = true, voiceLanguage = language) }
    fun stopVoice()          = setState { copy(isVoiceActive = false) }
    fun toggleVoiceLanguage() = setState {
        copy(voiceLanguage = if (voiceLanguage == KeyboardLanguage.EN) KeyboardLanguage.BN else KeyboardLanguage.EN)
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun toggleDarkTheme() = setState { copy(isDarkTheme = !isDarkTheme) }
    fun toggleHaptic()    = setState { copy(isHapticEnabled = !isHapticEnabled) }

    // ── Clipboard / Credentials actions ──────────────────────────────────────

    fun pasteFromClipboard(text: String) { insert(text); hideOverlay() }
    fun pasteFromCredential(text: String) { insert(text); hideOverlay() }

    fun saveClipToCredentials(clip: ClipboardEntry) {
        viewModelScope.launch { saveToCredentialsUseCase(clip) }
    }
}
