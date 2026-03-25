package com.aikeyboard.domain.model

import com.aikeyboard.core.constants.AppConstants

/**
 * Domain model for supported languages
 */
enum class Language(
    val code: String,
    val displayName: String,
    val nativeName: String,
    val localeCode: String
) {
    ENGLISH(
        code = AppConstants.LANG_ENGLISH,
        displayName = "English",
        nativeName = "English",
        localeCode = AppConstants.LANG_ENGLISH_FULL
    ),
    BENGALI(
        code = AppConstants.LANG_BENGALI,
        displayName = "Bengali",
        nativeName = "বাংলা",
        localeCode = AppConstants.LANG_BENGALI_FULL
    );

    companion object {
        /**
         * Get language from code
         */
        fun fromCode(code: String): Language {
            return entries.find { it.code == code } ?: ENGLISH
        }

        /**
         * Get all available languages
         */
        fun getAll(): List<Language> = entries

        /**
         * Get language pairs for translation
         */
        fun getLanguagePairs(): List<Pair<Language, Language>> {
            return listOf(
                ENGLISH to BENGALI,
                BENGALI to ENGLISH
            )
        }
    }

    /**
     * Check if this is a right-to-left language
     */
    val isRtl: Boolean
        get() = false // Bengali is not RTL

    /**
     * Get display text for translation direction
     */
    fun getTranslationDirectionText(target: Language): String {
        return "${nativeName} → ${target.nativeName}"
    }
}
