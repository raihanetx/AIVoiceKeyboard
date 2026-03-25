package com.aikeyboard.presentation.keyboard.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.KeyboardUiState

/**
 * Custom keyboard view for text input
 * 
 * Displays a keyboard layout with keys for the current language.
 */
class KeyboardView(
    context: Context,
    private val onKeyPress: (String) -> Unit,
    private val onLongKeyPress: (String) -> Unit
) : LinearLayout(context) {

    private var currentLanguage: Language = Language.ENGLISH
    private var capsLock: Boolean = false

    init {
        orientation = VERTICAL
        setPadding(0, context.dpToPx(4), 0, context.dpToPx(4))
    }

    /**
     * Update the keyboard layout based on state
     */
    fun updateState(state: KeyboardUiState) {
        currentLanguage = state.currentLanguage
        capsLock = state.capsLock
        rebuildKeyboard()
    }

    /**
     * Rebuild the keyboard layout
     */
    private fun rebuildKeyboard() {
        removeAllViews()

        val rows = if (currentLanguage == Language.ENGLISH) {
            getEnglishKeyboardRows()
        } else {
            getBengaliKeyboardRows()
        }

        rows.forEach { row ->
            addView(createKeyboardRow(row))
        }
    }

    /**
     * Create a row of keyboard keys
     */
    private fun createKeyboardRow(keys: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            keys.forEach { key ->
                addView(createKeyButton(key))
            }
        }
    }

    /**
     * Create a single key button
     */
    private fun createKeyButton(key: String): Button {
        return Button(context).apply {
            text = if (capsLock && key.length == 1) key.uppercase() else key
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            textSize = 16f
            setAllCaps(false)

            val width = when {
                key == "Space" -> 150
                key == "⇧" || key == "⌫" -> 50
                key == "?123" || key == "😊" || key == "." -> 45
                else -> 35
            }

            layoutParams = LayoutParams(
                context.dpToPx(width),
                context.dpToPx(AppConstants.KEYBOARD_ROW_HEIGHT_DP)
            ).apply {
                setMargins(
                    context.dpToPx(AppConstants.KEYBOARD_KEY_MARGIN_DP),
                    context.dpToPx(AppConstants.KEYBOARD_KEY_MARGIN_DP),
                    context.dpToPx(AppConstants.KEYBOARD_KEY_MARGIN_DP),
                    context.dpToPx(AppConstants.KEYBOARD_KEY_MARGIN_DP)
                )
            }

            setOnClickListener { onKeyPress(key) }
            setOnLongClickListener {
                onLongKeyPress(key)
                true
            }
        }
    }

    /**
     * Get English keyboard layout
     */
    private fun getEnglishKeyboardRows(): List<List<String>> {
        return listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫"),
            listOf("?123", "😊", "Space", ".", "↵")
        )
    }

    /**
     * Get Bengali keyboard layout
     */
    private fun getBengaliKeyboardRows(): List<List<String>> {
        return listOf(
            listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০"),
            listOf("ৌ", "ৈ", "া", "ী", "ূ", "ব", "হ", "গ", "দ", "জ"),
            listOf("ো", "ে", "্", "ি", "ু", "প", "র", "ক", "ত", "চ"),
            listOf("ং", "ম", "ন", "ণ", "স", "ও", "য", "শ", "খ", "ঃ")
        )
    }

    companion object {
        /**
         * Create a keyboard view
         */
        fun create(
            context: Context,
            onKeyPress: (String) -> Unit,
            onLongKeyPress: (String) -> Unit = {}
        ): KeyboardView {
            return KeyboardView(context, onKeyPress, onLongKeyPress)
        }
    }
}
