package com.aikeyboard.presentation.keyboard.view

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.KeyboardUiState

/**
 * Translation view for text translation
 * 
 * Displays a translation panel with input field and result display.
 */
class TranslateView(
    context: Context,
    private val onTranslate: (String, Language, Language) -> Unit,
    private val onLanguageChange: (Language) -> Unit,
    private val onInsertText: (String) -> Unit
) : LinearLayout(context) {

    private var inputEditText: EditText? = null
    private var resultTextView: TextView? = null
    private var resultCard: LinearLayout? = null

    init {
        orientation = VERTICAL
        setPadding(
            context.dpToPx(16),
            context.dpToPx(12),
            context.dpToPx(16),
            context.dpToPx(12)
        )
    }

    /**
     * Update the view based on state
     */
    fun updateState(state: KeyboardUiState) {
        if (childCount == 0) {
            buildView(state)
        } else {
            updateExistingView(state)
        }
    }

    /**
     * Build the complete translation view
     */
    private fun buildView(state: KeyboardUiState) {
        // Language direction switcher
        addView(createLanguageDirectionSwitcher(state))

        // Input field
        inputEditText = createInputEditText()
        addView(inputEditText)

        // Translate button
        addView(createTranslateButton(state))

        // Result card
        resultCard = createResultCard()
        addView(resultCard)
    }

    /**
     * Update existing view elements
     */
    private fun updateExistingView(state: KeyboardUiState) {
        // Update result card
        if (state.showResultCard && state.resultTextToShow != null) {
            resultTextView?.text = state.resultTextToShow
            resultCard?.visibility = View.VISIBLE
        }
    }

    /**
     * Create language direction switcher
     */
    private fun createLanguageDirectionSwitcher(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER

            addView(createChip(
                text = "EN → বাং",
                isSelected = state.currentLanguage == Language.ENGLISH,
                onClick = { onLanguageChange(Language.ENGLISH) }
            ))
            addView(View(context).apply { 
                layoutParams = LayoutParams(context.dpToPx(16), 1) 
            })
            addView(createChip(
                text = "বাং → EN",
                isSelected = state.currentLanguage == Language.BENGALI,
                onClick = { onLanguageChange(Language.BENGALI) }
            ))
        }
    }

    /**
     * Create input text field
     */
    private fun createInputEditText(): EditText {
        return EditText(context).apply {
            hint = "Enter text to translate"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
            textSize = 14f
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setPadding(
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12)
            )
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, context.dpToPx(12), 0, context.dpToPx(8))
            }
        }
    }

    /**
     * Create translate button
     */
    private fun createTranslateButton(state: KeyboardUiState): Button {
        return Button(context).apply {
            text = "Translate"
            setBackgroundColor(Color.parseColor("#4285F4"))
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                context.dpToPx(40)
            )
            setOnClickListener {
                val input = inputEditText?.text?.toString() ?: ""
                if (input.isNotBlank()) {
                    val targetLanguage = if (state.currentLanguage == Language.ENGLISH) {
                        Language.BENGALI
                    } else {
                        Language.ENGLISH
                    }
                    onTranslate(input, state.currentLanguage, targetLanguage)
                } else {
                    Toast.makeText(context, "Please enter text to translate", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Create result card
     */
    private fun createResultCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            setPadding(
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12)
            )
            visibility = View.GONE
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, context.dpToPx(12), 0, 0)
            }

            resultTextView = TextView(context).apply {
                setTextColor(Color.WHITE)
                textSize = 14f
            }
            addView(resultTextView)

            addView(Button(context).apply {
                text = "Insert"
                setTextColor(Color.parseColor("#4285F4"))
                setBackgroundColor(Color.TRANSPARENT)
                textSize = 12f
                setOnClickListener {
                    resultTextView?.text?.toString()?.let { text ->
                        if (text.isNotBlank()) {
                            onInsertText(text)
                        }
                    }
                }
            })
        }
    }

    /**
     * Create a chip button
     */
    private fun createChip(
        text: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ): Button {
        return Button(context).apply {
            this.text = text
            textSize = 11f
            setTextColor(if (isSelected) Color.WHITE else Color.GRAY)
            setBackgroundColor(
                if (isSelected) Color.parseColor("#4285F4") 
                else Color.TRANSPARENT
            )
            setPadding(
                context.dpToPx(12),
                context.dpToPx(6),
                context.dpToPx(12),
                context.dpToPx(6)
            )
            setAllCaps(false)
            setOnClickListener { onClick() }
        }
    }

    companion object {
        /**
         * Create a translate view
         */
        fun create(
            context: Context,
            onTranslate: (String, Language, Language) -> Unit,
            onLanguageChange: (Language) -> Unit,
            onInsertText: (String) -> Unit
        ): TranslateView {
            return TranslateView(context, onTranslate, onLanguageChange, onInsertText)
        }
    }
}
