package com.aikeyboard.presentation.keyboard.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.ApiKeyStatus
import com.aikeyboard.presentation.keyboard.VoiceUiState

/**
 * Voice Input View - Main voice typing panel
 */
class VoiceInputView(context: Context) : LinearLayout(context) {

    // Callbacks
    var onEngineSelected: ((String) -> Unit)? = null
    var onApiKeySaved: ((engine: String, apiKey: String) -> Unit)? = null
    var onMicClicked: (() -> Unit)? = null
    var onLanguageChanged: ((Language) -> Unit)? = null
    var onInsertText: ((String) -> Unit)? = null

    // Engine cards
    private lateinit var androidCard: EngineCardView
    private lateinit var groqCard: EngineCardView
    private lateinit var geminiCard: EngineCardView

    // Other UI
    private lateinit var micButton: FrameLayout
    private lateinit var statusText: TextView
    private lateinit var errorDisplay: ErrorDisplayView
    private lateinit var resultCard: LinearLayout
    private lateinit var resultTextView: TextView
    private lateinit var englishButton: Button
    private lateinit var bengaliButton: Button

    // Current state
    private var currentState: VoiceUiState? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.background))
        setPadding(
            context.dpToPx(12),
            context.dpToPx(12),
            context.dpToPx(12),
            context.dpToPx(12)
        )
        gravity = Gravity.CENTER_HORIZONTAL

        buildUI()
    }

    private fun buildUI() {
        // Title
        addView(TextView(context).apply {
            text = "🎤 Voice Typing"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(8))
        })

        // Android card
        androidCard = EngineCardView(
            context = context,
            engineCode = "android",
            engineName = "Android (Offline)",
            engineEmoji = "🟢",
            description = "Free • Works without internet",
            engineColor = ContextCompat.getColor(context, R.color.engine_android),
            needsApiKey = false
        )
        androidCard.onSelected = {
            Log.d(TAG, "Android card selected")
            onEngineSelected?.invoke("android")
        }
        addView(androidCard)

        // Groq card
        groqCard = EngineCardView(
            context = context,
            engineCode = "groq",
            engineName = "Groq (Online)",
            engineEmoji = "🔵",
            description = "Fast & accurate transcription",
            engineColor = ContextCompat.getColor(context, R.color.engine_groq),
            needsApiKey = true
        )
        groqCard.onSelected = {
            Log.d(TAG, "Groq card selected")
            onEngineSelected?.invoke("groq")
        }
        groqCard.onApiKeyEntered = { key ->
            Log.d(TAG, "Groq API key entered: ${key.take(5)}...")
            onApiKeySaved?.invoke("groq", key)
        }
        addView(groqCard)

        // Gemini card
        geminiCard = EngineCardView(
            context = context,
            engineCode = "gemini",
            engineName = "Gemini (Live)",
            engineEmoji = "🟣",
            description = "Real-time streaming transcription",
            engineColor = ContextCompat.getColor(context, R.color.engine_gemini),
            needsApiKey = true
        )
        geminiCard.onSelected = {
            Log.d(TAG, "Gemini card selected")
            onEngineSelected?.invoke("gemini")
        }
        geminiCard.onApiKeyEntered = { key ->
            Log.d(TAG, "Gemini API key entered: ${key.take(5)}...")
            onApiKeySaved?.invoke("gemini", key)
        }
        addView(geminiCard)

        // Error display
        errorDisplay = ErrorDisplayView.create(context)
        addView(errorDisplay)

        // Language switcher
        addView(LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, context.dpToPx(8), 0, context.dpToPx(12))
            }

            englishButton = Button(context).apply {
                text = "English"
                textSize = 12f
                isAllCaps = false
                setPadding(
                    context.dpToPx(16),
                    context.dpToPx(8),
                    context.dpToPx(16),
                    context.dpToPx(8)
                )
                background = createRoundedBackground(Color.parseColor("#252540"))
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setOnClickListener { onLanguageChanged?.invoke(Language.ENGLISH) }
            }
            addView(englishButton)

            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(16), 1)
            })

            bengaliButton = Button(context).apply {
                text = "বাংলা"
                textSize = 12f
                isAllCaps = false
                setPadding(
                    context.dpToPx(16),
                    context.dpToPx(8),
                    context.dpToPx(16),
                    context.dpToPx(8)
                )
                background = createRoundedBackground(Color.parseColor("#252540"))
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                setOnClickListener { onLanguageChanged?.invoke(Language.BENGALI) }
            }
            addView(bengaliButton)
        })

        // Mic button
        micButton = FrameLayout(context).apply {
            layoutParams = LayoutParams(context.dpToPx(80), context.dpToPx(80)).apply {
                setMargins(0, 0, 0, context.dpToPx(8))
            }

            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    context.dpToPx(72),
                    context.dpToPx(72),
                    Gravity.CENTER
                )
                background = createRoundedBackground(
                    ContextCompat.getColor(context, R.color.engine_android),
                    36f
                )

                addView(TextView(context).apply {
                    text = "🎤"
                    textSize = 32f
                    gravity = Gravity.CENTER
                })
            })

            // Click listener on the FrameLayout (outer container)
            isClickable = true
            isFocusable = true
            setOnClickListener {
                Log.d(TAG, "=== MIC BUTTON CLICKED ===")
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    Log.d(TAG, "Permission granted, invoking onMicClicked")
                    onMicClicked?.invoke()
                } else {
                    Log.w(TAG, "Permission NOT granted!")
                    showError("Permission Required", "Microphone permission is required for voice typing.", null)
                }
            }
        }
        addView(micButton)

        // Status text
        statusText = TextView(context).apply {
            text = "Select an engine and tap to speak"
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 13f
            gravity = Gravity.CENTER
        }
        addView(statusText)

        // Result card
        resultCard = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            background = createRoundedBackground(
                ContextCompat.getColor(context, R.color.success_green),
                8f
            )
            setPadding(
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12)
            )
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, context.dpToPx(12), 0, 0)
            }

            resultTextView = TextView(context).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 14f
            }
            addView(resultTextView)

            addView(Button(context).apply {
                text = "✓ Insert Text"
                isAllCaps = false
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.success_green))
                setBackgroundColor(Color.WHITE)
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, context.dpToPx(8), 0, 0)
                }
                setOnClickListener {
                    resultTextView.text?.toString()?.let { text ->
                        if (text.isNotBlank()) {
                            onInsertText?.invoke(text)
                            visibility = View.GONE
                        }
                    }
                }
            })
        }
        addView(resultCard)
    }

    /**
     * Update state from service
     */
    fun updateState(state: VoiceUiState) {
        Log.d(TAG, "updateState: engine=${state.selectedEngine}, groqStatus=${state.groqApiKeyStatus}")
        currentState = state

        // Update engine cards
        androidCard.setEngineSelected(state.isAndroidSelected)
        groqCard.setEngineSelected(state.isGroqSelected)
        groqCard.setApiKeyStatus(state.groqApiKeyStatus)
        geminiCard.setEngineSelected(state.isGeminiSelected)
        geminiCard.setApiKeyStatus(state.geminiApiKeyStatus)

        // Show/hide API key input
        when (state.showApiKeyInputFor) {
            "groq" -> groqCard.showApiKeyInput()
            "gemini" -> geminiCard.showApiKeyInput()
            else -> {
                groqCard.hideApiKeyInput()
                geminiCard.hideApiKeyInput()
            }
        }

        // Update language buttons
        if (state.currentLanguage == Language.ENGLISH) {
            englishButton.background = createRoundedBackground(ContextCompat.getColor(context, R.color.primary))
            englishButton.setTextColor(Color.WHITE)
            bengaliButton.background = createRoundedBackground(Color.parseColor("#252540"))
            bengaliButton.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        } else {
            bengaliButton.background = createRoundedBackground(ContextCompat.getColor(context, R.color.primary))
            bengaliButton.setTextColor(Color.WHITE)
            englishButton.background = createRoundedBackground(Color.parseColor("#252540"))
            englishButton.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        // Update mic button color
        val color = if (state.isRecording) {
            Color.parseColor("#FF5722")
        } else {
            when (state.selectedEngine) {
                "android" -> ContextCompat.getColor(context, R.color.engine_android)
                "groq" -> ContextCompat.getColor(context, R.color.engine_groq)
                "gemini" -> ContextCompat.getColor(context, R.color.engine_gemini)
                else -> Color.GRAY
            }
        }
        (micButton.getChildAt(0) as? LinearLayout)?.background = createRoundedBackground(color, 36f)

        // Update status text
        statusText.text = state.statusMessage

        // Handle error display
        if (state.errorMessage != null || state.errorType != null) {
            showError(
                title = state.errorTitle ?: "Error",
                message = state.errorMessage ?: "Unknown error",
                details = state.errorDetails
            )
        } else {
            hideError()
        }

        // Update result card
        if (state.showResult && state.resultText != null) {
            resultTextView.text = state.resultText
            resultCard.visibility = View.VISIBLE
            hideError()
        } else {
            resultCard.visibility = View.GONE
        }
    }

    /**
     * Show error in the error display
     */
    fun showError(title: String, message: String, details: String?) {
        errorDisplay.showError(title, message, details)
    }

    /**
     * Show API error with type
     */
    fun showApiError(engine: String, errorType: String, message: String, fullError: String?) {
        errorDisplay.showApiError(engine, errorType, message, fullError)
    }

    /**
     * Hide error display
     */
    fun hideError() {
        errorDisplay.hideError()
    }

    private fun createRoundedBackground(color: Int, cornerRadiusDp: Float = 8f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            this.cornerRadius = context.dpToPx(cornerRadiusDp.toInt()).toFloat()
        }
    }

    companion object {
        private const val TAG = "VoiceInputView"

        fun create(context: Context): VoiceInputView {
            return VoiceInputView(context)
        }
    }
}
