package com.aikeyboard.presentation.keyboard.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
 *
 * Shows 3 engine options (Android, Groq, Gemini) with proper selection
 * and API key input functionality.
 */
class VoiceInputView(
    context: Context
) : LinearLayout(context) {

    // Callbacks
    var onEngineSelected: ((String) -> Unit)? = null
    var onApiKeyEntered: ((String, String) -> Unit)? = null
    var onMicClicked: (() -> Unit)? = null
    var onLanguageChanged: ((Language) -> Unit)? = null
    var onInsertText: ((String) -> Unit)? = null

    // Engine cards
    private var androidCard: EngineCardView
    private var groqCard: EngineCardView
    private var geminiCard: EngineCardView

    // Other UI components
    private lateinit var micButton: FrameLayout
    private lateinit var statusText: TextView
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

        // Title
        addView(createTitle())

        // Engine cards container
        addView(LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, context.dpToPx(8), 0, context.dpToPx(12))
            }

            // Android card - no API key needed
            androidCard = EngineCardView(
                context = context,
                engineCode = "android",
                engineName = "Android (Offline)",
                engineEmoji = "🟢",
                description = "Free • Works without internet",
                engineColor = ContextCompat.getColor(context, R.color.engine_android),
                needsApiKey = false
            )
            androidCard.onSelected = { handleEngineSelection("android") }
            addView(androidCard)

            // Groq card - needs API key
            groqCard = EngineCardView(
                context = context,
                engineCode = "groq",
                engineName = "Groq (Online)",
                engineEmoji = "🔵",
                description = "Fast & accurate transcription",
                engineColor = ContextCompat.getColor(context, R.color.engine_groq),
                needsApiKey = true
            )
            groqCard.onSelected = { handleEngineSelection("groq") }
            groqCard.onApiKeyEntered = { key -> onApiKeyEntered?.invoke("groq", key) }
            addView(groqCard)

            // Gemini card - needs API key
            geminiCard = EngineCardView(
                context = context,
                engineCode = "gemini",
                engineName = "Gemini (Live)",
                engineEmoji = "🟣",
                description = "Real-time streaming transcription",
                engineColor = ContextCompat.getColor(context, R.color.engine_gemini),
                needsApiKey = true
            )
            geminiCard.onSelected = { handleEngineSelection("gemini") }
            geminiCard.onApiKeyEntered = { key -> onApiKeyEntered?.invoke("gemini", key) }
            addView(geminiCard)
        })

        // Language switcher
        addView(createLanguageSwitcher())

        // Mic button
        micButton = createMicButton()
        addView(micButton)

        // Status text
        statusText = createStatusText()
        addView(statusText)

        // Result card
        resultCard = createResultCard()
        addView(resultCard)
    }

    /**
     * Update the view with new state
     */
    fun updateState(state: VoiceUiState) {
        currentState = state

        // Update engine cards
        androidCard.setEngineSelected(state.isAndroidSelected)
        groqCard.setEngineSelected(state.isGroqSelected)
        groqCard.setApiKeyStatus(state.groqApiKeyStatus)
        geminiCard.setEngineSelected(state.isGeminiSelected)
        geminiCard.setApiKeyStatus(state.geminiApiKeyStatus)

        // Show/hide API key input for specific engine
        when (state.showApiKeyInputFor) {
            "groq" -> groqCard.showApiKeyInput()
            "gemini" -> geminiCard.showApiKeyInput()
            else -> {
                groqCard.hideApiKeyInput()
                geminiCard.hideApiKeyInput()
            }
        }

        // Update language buttons
        updateLanguageButtons(state.currentLanguage)

        // Update mic button color
        updateMicButtonColor(state.selectedEngine, state.isRecording)

        // Update status text
        statusText.text = state.statusMessage
        if (state.errorMessage != null) {
            statusText.setTextColor(Color.parseColor("#FF5252"))
        } else {
            statusText.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        // Update result card
        if (state.showResult && state.resultText != null) {
            resultTextView.text = state.resultText
            resultCard.visibility = View.VISIBLE
        } else {
            resultCard.visibility = View.GONE
        }
    }

    /**
     * Handle engine selection with validation
     */
    private fun handleEngineSelection(engine: String) {
        onEngineSelected?.invoke(engine)
    }

    /**
     * Create title TextView
     */
    private fun createTitle(): TextView {
        return TextView(context).apply {
            text = "🎤 Voice Typing"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(8))
        }
    }

    /**
     * Create language switcher
     */
    private fun createLanguageSwitcher(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, context.dpToPx(12))
            }

            englishButton = createLanguageButton("English")
            englishButton.setOnClickListener { onLanguageChanged?.invoke(Language.ENGLISH) }
            addView(englishButton)

            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(16), 1)
            })

            bengaliButton = createLanguageButton("বাংলা")
            bengaliButton.setOnClickListener { onLanguageChanged?.invoke(Language.BENGALI) }
            addView(bengaliButton)
        }
    }

    private fun createLanguageButton(text: String): Button {
        return Button(context).apply {
            this.text = text
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
        }
    }

    private fun updateLanguageButtons(language: Language) {
        if (language == Language.ENGLISH) {
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
    }

    /**
     * Create mic button
     */
    private fun createMicButton(): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LayoutParams(context.dpToPx(80), context.dpToPx(80)).apply {
                setMargins(0, 0, 0, context.dpToPx(8))
            }

            // Background circle
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
                    textSize = 28f
                    gravity = Gravity.CENTER
                })

                setOnClickListener {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        onMicClicked?.invoke()
                    } else {
                        Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun updateMicButtonColor(engine: String, isRecording: Boolean) {
        val color = if (isRecording) {
            Color.parseColor("#FF5722")
        } else {
            when (engine) {
                "android" -> ContextCompat.getColor(context, R.color.engine_android)
                "groq" -> ContextCompat.getColor(context, R.color.engine_groq)
                "gemini" -> ContextCompat.getColor(context, R.color.engine_gemini)
                else -> Color.GRAY
            }
        }

        (micButton.getChildAt(0) as? LinearLayout)?.background = createRoundedBackground(color, 36f)
    }

    /**
     * Create status text
     */
    private fun createStatusText(): TextView {
        return TextView(context).apply {
            text = "Select an engine and tap to speak"
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 12f
            gravity = Gravity.CENTER
        }
    }

    /**
     * Create result card
     */
    private fun createResultCard(): LinearLayout {
        return LinearLayout(context).apply {
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
    }

    // Helper
    private fun createRoundedBackground(color: Int, cornerRadiusDp: Float = 8f): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            this.cornerRadius = context.dpToPx(cornerRadiusDp.toInt()).toFloat()
        }
    }

    companion object {
        fun create(context: Context): VoiceInputView {
            return VoiceInputView(context)
        }
    }
}
