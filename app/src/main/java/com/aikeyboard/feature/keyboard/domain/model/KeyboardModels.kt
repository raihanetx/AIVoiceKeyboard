package com.aikeyboard.feature.keyboard.domain.model

/** Which language layout is active. */
enum class KeyboardLanguage { EN, BN }

/** Alpha (letter) vs numeric/symbol mode. */
enum class KeyboardMode { ALPHA, NUM }

/** Which panel is slid up over the keyboard. */
enum class OverlayType { NONE, EMOJI, CLIPBOARD, CREDENTIALS, SETTINGS }

/**
 * Single source of truth for the entire keyboard UI.
 * Held and updated exclusively inside [KeyboardViewModel].
 */
data class KeyboardState(
    val language        : KeyboardLanguage = KeyboardLanguage.EN,
    val mode            : KeyboardMode     = KeyboardMode.ALPHA,
    val isCaps          : Boolean          = false,
    val bufferText      : String           = "",
    val isDarkTheme     : Boolean          = false,
    val isHapticEnabled : Boolean          = true,
    val activeOverlay   : OverlayType      = OverlayType.NONE,
    val isVoiceActive   : Boolean          = false,
    val voiceLanguage   : KeyboardLanguage = KeyboardLanguage.EN,
    val suggestions     : List<String>     = emptyList(),
)
