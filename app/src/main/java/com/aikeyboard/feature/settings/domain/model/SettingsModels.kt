package com.aikeyboard.feature.settings.domain.model

/** All user-configurable preferences. */
data class SettingsState(
    val isDarkTheme     : Boolean = false,
    val isHapticEnabled : Boolean = true,
    val voiceEngineEn   : VoiceEngine = VoiceEngine.OFFLINE,
    val voiceEngineBn   : VoiceEngine = VoiceEngine.OFFLINE,
)

enum class VoiceEngine(val label: String) {
    OFFLINE("Android Offline"),
    GROQ("Groq System"),
}
