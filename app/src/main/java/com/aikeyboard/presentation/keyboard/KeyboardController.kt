package com.aikeyboard.presentation.keyboard

import android.util.Log
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.model.TranslationResult

private const val TAG = "KeyboardController"

/**
 * Controller for keyboard state management
 * 
 * Handles state changes and business logic for the keyboard UI.
 * This class is designed to be used within the keyboard service.
 */
class KeyboardController {

    // Current state
    private var _state = KeyboardUiState()
    val state: KeyboardUiState get() = _state

    // Listeners for state changes
    private var onStateChanged: ((KeyboardUiState) -> Unit)? = null

    /**
     * Set the state change listener
     */
    fun setOnStateChangedListener(listener: (KeyboardUiState) -> Unit) {
        onStateChanged = listener
    }

    /**
     * Update state and notify listener
     */
    private fun updateState(newState: KeyboardUiState) {
        _state = newState
        onStateChanged?.invoke(_state)
    }

    // ==================== Panel Management ====================

    /**
     * Switch to a different panel
     */
    fun switchPanel(panel: String) {
        Log.d(TAG, "Switching to panel: $panel")
        updateState(_state.copy(currentPanel = panel))
    }

    /**
     * Switch to keyboard panel
     */
    fun showKeyboard() = switchPanel(AppConstants.PANEL_KEYBOARD)

    /**
     * Switch to voice panel
     */
    fun showVoice() = switchPanel(AppConstants.PANEL_VOICE)

    /**
     * Switch to translate panel
     */
    fun showTranslate() = switchPanel(AppConstants.PANEL_TRANSLATE)

    /**
     * Switch to emoji panel
     */
    fun showEmoji() = switchPanel(AppConstants.PANEL_EMOJI)

    // ==================== Language Management ====================

    /**
     * Set the current language
     */
    fun setLanguage(language: Language) {
        Log.d(TAG, "Setting language: ${language.code}")
        updateState(_state.copy(currentLanguage = language))
    }

    /**
     * Set the current language by code
     */
    fun setLanguage(languageCode: String) {
        setLanguage(Language.fromCode(languageCode))
    }

    /**
     * Toggle between English and Bengali
     */
    fun toggleLanguage(): Language {
        val newLanguage = if (_state.currentLanguage == Language.ENGLISH) {
            Language.BENGALI
        } else {
            Language.ENGLISH
        }
        setLanguage(newLanguage)
        return newLanguage
    }

    // ==================== Text Management ====================

    /**
     * Append text to the current input
     */
    fun appendText(text: String) {
        val newText = StringBuilder(_state.currentText).append(text)
        updateState(_state.copy(currentText = newText))
    }

    /**
     * Delete the last character
     */
    fun deleteLastChar() {
        if (_state.currentText.isNotEmpty()) {
            val newText = StringBuilder(_state.currentText).apply {
                deleteCharAt(length - 1)
            }
            updateState(_state.copy(currentText = newText))
        }
    }

    /**
     * Clear all text
     */
    fun clearText() {
        updateState(_state.copy(currentText = StringBuilder()))
    }

    /**
     * Toggle caps lock
     */
    fun toggleCapsLock() {
        updateState(_state.copy(capsLock = !_state.capsLock))
        Log.d(TAG, "Caps lock: ${_state.capsLock}")
    }

    // ==================== Voice Recording Management ====================

    /**
     * Set the STT engine
     */
    fun setSttEngine(engine: String) {
        updateState(_state.copy(sttEngine = engine))
        Log.d(TAG, "STT engine set to: $engine")
    }

    /**
     * Start recording
     */
    fun startRecording() {
        updateState(_state.copy(
            isRecording = true,
            voiceStatusText = "Listening...",
            transcriptionResult = null
        ))
    }

    /**
     * Stop recording
     */
    fun stopRecording() {
        updateState(_state.copy(
            isRecording = false,
            voiceStatusText = "Processing..."
        ))
    }

    /**
     * Set recording status
     */
    fun setRecordingStatus(isRecording: Boolean, statusText: String? = null) {
        updateState(_state.copy(
            isRecording = isRecording,
            voiceStatusText = statusText ?: if (isRecording) "Recording..." else "Tap mic to speak"
        ))
    }

    /**
     * Set transcription result
     */
    fun setTranscriptionResult(result: TranscriptionResult) {
        val statusText = when (result) {
            is TranscriptionResult.Success -> "Done!"
            is TranscriptionResult.Error -> "Error: ${result.message}"
            is TranscriptionResult.Loading -> "Processing..."
        }
        updateState(_state.copy(
            transcriptionResult = result,
            isRecording = false,
            voiceStatusText = statusText
        ))
    }

    /**
     * Clear transcription result
     */
    fun clearTranscriptionResult() {
        updateState(_state.copy(
            transcriptionResult = null,
            voiceStatusText = "Tap mic to speak"
        ))
    }

    // ==================== Translation Management ====================

    /**
     * Set translation input
     */
    fun setTranslationInput(input: String) {
        updateState(_state.copy(translationInput = input))
    }

    /**
     * Set translating state
     */
    fun setTranslating(isTranslating: Boolean) {
        updateState(_state.copy(isTranslating = isTranslating))
    }

    /**
     * Set translation result
     */
    fun setTranslationResult(result: TranslationResult) {
        updateState(_state.copy(
            translationResult = result,
            isTranslating = false
        ))
    }

    /**
     * Clear translation result
     */
    fun clearTranslationResult() {
        updateState(_state.copy(translationResult = null))
    }

    // ==================== Error Management ====================

    /**
     * Set error message
     */
    fun setError(message: String) {
        Log.e(TAG, "Error: $message")
        updateState(_state.copy(errorMessage = message))
    }

    /**
     * Clear error
     */
    fun clearError() {
        updateState(_state.copy(errorMessage = null))
    }

    // ==================== Utility Methods ====================

    /**
     * Reset state to default
     */
    fun reset() {
        _state = KeyboardUiState()
        onStateChanged?.invoke(_state)
    }

    /**
     * Get the target language for translation
     * (Opposite of current language)
     */
    fun getTargetLanguageForTranslation(): Language {
        return if (_state.currentLanguage == Language.ENGLISH) {
            Language.BENGALI
        } else {
            Language.ENGLISH
        }
    }
}
