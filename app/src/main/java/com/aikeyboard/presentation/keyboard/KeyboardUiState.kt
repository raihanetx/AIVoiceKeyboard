package com.aikeyboard.presentation.keyboard

import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.domain.model.Language
import com.aikeyboard.domain.model.TranscriptionResult
import com.aikeyboard.domain.model.TranslationResult

/**
 * UI State for the keyboard
 * 
 * Represents all the state needed to render the keyboard UI.
 */
data class KeyboardUiState(
    // Current panel
    val currentPanel: String = AppConstants.PANEL_KEYBOARD,
    
    // Language state
    val currentLanguage: Language = Language.ENGLISH,
    
    // Text state
    val currentText: StringBuilder = StringBuilder(),
    val capsLock: Boolean = false,
    
    // Voice recording state
    val isRecording: Boolean = false,
    val sttEngine: String = AppConstants.STT_ENGINE_ANDROID,
    val transcriptionResult: TranscriptionResult? = null,
    val voiceStatusText: String = "Tap mic to speak",
    
    // Translation state
    val translationInput: String = "",
    val translationResult: TranslationResult? = null,
    val isTranslating: Boolean = false,
    
    // Error state
    val errorMessage: String? = null
) {
    /**
     * Check if voice panel is active
     */
    val isVoicePanel: Boolean
        get() = currentPanel == AppConstants.PANEL_VOICE

    /**
     * Check if translate panel is active
     */
    val isTranslatePanel: Boolean
        get() = currentPanel == AppConstants.PANEL_TRANSLATE

    /**
     * Check if emoji panel is active
     */
    val isEmojiPanel: Boolean
        get() = currentPanel == AppConstants.PANEL_EMOJI

    /**
     * Check if keyboard panel is active
     */
    val isKeyboardPanel: Boolean
        get() = currentPanel == AppConstants.PANEL_KEYBOARD

    /**
     * Check if using Android STT
     */
    val isUsingAndroidStt: Boolean
        get() = sttEngine == AppConstants.STT_ENGINE_ANDROID

    /**
     * Check if using Groq STT
     */
    val isUsingGroqStt: Boolean
        get() = sttEngine == AppConstants.STT_ENGINE_GROQ

    /**
     * Check if using Google STT
     */
    val isUsingGeminiStt: Boolean
        get() = sttEngine == AppConstants.STT_ENGINE_GEMINI

    /**
     * Get the text to show in the result card
     */
    val resultTextToShow: String?
        get() = transcriptionResult?.getTextOrNull() ?: translationResult?.getTextOrNull()

    /**
     * Check if result card should be visible
     */
    val showResultCard: Boolean
        get() = transcriptionResult?.isSuccess == true || translationResult?.isSuccess == true
}
