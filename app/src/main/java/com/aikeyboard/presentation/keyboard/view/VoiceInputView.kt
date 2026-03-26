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
import androidx.core.view.setPadding
import com.aikeyboard.R
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.KeyboardUiState

/**
 * Voice input view for speech-to-text
 *
 * Displays a voice input panel with recording controls and result display.
 * Supports 3 STT engines: Android (Offline), Groq (Online), Gemini (Live)
 */
class VoiceInputView(
    context: Context,
    private val onMicClick: () -> Unit,
    private val onEngineChange: (String) -> Unit,
    private val onLanguageChange: (Language) -> Unit,
    private val onInsertText: (String) -> Unit
) : LinearLayout(context) {

    private var activeBanner: LinearLayout? = null
    private var activeBannerText: TextView? = null
    private var activeBannerIcon: TextView? = null

    private var statusText: TextView? = null
    private var resultText: TextView? = null
    private var resultCard: LinearLayout? = null
    private var micButton: FrameLayout? = null
    private var micIcon: TextView? = null
    private var micEngineIndicator: View? = null

    // Engine cards
    private var androidCard: LinearLayout? = null
    private var groqCard: LinearLayout? = null
    private var geminiCard: LinearLayout? = null

    // Engine switches
    private var androidSwitch: Switch? = null
    private var groqSwitch: Switch? = null
    private var geminiSwitch: Switch? = null

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
     * Build the complete voice input view
     */
    private fun buildView(state: KeyboardUiState) {
        // Title
        addView(createTitle())

        // Active Engine Banner
        activeBanner = createActiveBanner(state)
        addView(activeBanner)

        // Engine selection cards
        addView(createEngineCardsSection(state))

        // Language switcher
        addView(createLanguageSwitcher(state))

        // Mic button with engine indicator
        micButton = createMicButton(state)
        addView(micButton)

        // Status text
        statusText = createStatusText(state)
        addView(statusText)

        // Result card
        resultCard = createResultCard()
        addView(resultCard)
    }

    /**
     * Create section title
     */
    private fun createTitle(): TextView {
        return TextView(context).apply {
            text = "🎤 Voice Recognition"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(12))
        }
    }

    /**
     * Create active engine banner
     */
    private fun createActiveBanner(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.dpToPx(12),
                context.dpToPx(10),
                context.dpToPx(12),
                context.dpToPx(10)
            )
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, context.dpToPx(12))
            }

            // Set background with rounded corners
            background = createRoundedDrawable(
                fillColor = getEngineColorDark(state.sttEngine),
                cornerRadius = 8f
            )

            // Engine icon/badge
            activeBannerIcon = TextView(context).apply {
                text = getEngineEmoji(state.sttEngine)
                textSize = 16f
            }
            addView(activeBannerIcon)

            // Spacing
            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(8), 1)
            })

            // Active text
            activeBannerText = TextView(context).apply {
                text = "${getEngineName(state.sttEngine)} - READY"
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(activeBannerText)
        }
    }

    /**
     * Create engine cards section
     */
    private fun createEngineCardsSection(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, context.dpToPx(12))
            }

            // Section label
            addView(TextView(context).apply {
                text = "Select STT Engine"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                setPadding(0, 0, 0, context.dpToPx(8))
            })

            // Android Card
            androidCard = createEngineCard(
                engineCode = AppConstants.STT_ENGINE_ANDROID,
                emoji = "🟢",
                name = "Android (Offline)",
                description = "Works without internet, unlimited free",
                isSelected = state.isUsingAndroidStt,
                engineColor = ContextCompat.getColor(context, R.color.engine_android)
            )
            addView(androidCard)

            // Spacing between cards
            addView(View(context).apply {
                layoutParams = LayoutParams(1, context.dpToPx(6))
            })

            // Groq Card
            groqCard = createEngineCard(
                engineCode = AppConstants.STT_ENGINE_GROQ,
                emoji = "🔵",
                name = "Groq (Online)",
                description = "Fast & accurate, requires API key",
                isSelected = state.isUsingGroqStt,
                engineColor = ContextCompat.getColor(context, R.color.engine_groq)
            )
            addView(groqCard)

            // Spacing between cards
            addView(View(context).apply {
                layoutParams = LayoutParams(1, context.dpToPx(6))
            })

            // Gemini Card
            geminiCard = createEngineCard(
                engineCode = AppConstants.STT_ENGINE_GEMINI,
                emoji = "🟣",
                name = "Gemini (Live)",
                description = "Best quality streaming, requires API key",
                isSelected = state.isUsingGeminiStt,
                engineColor = ContextCompat.getColor(context, R.color.engine_gemini)
            )
            addView(geminiCard)
        }
    }

    /**
     * Create an engine selection card
     */
    private fun createEngineCard(
        engineCode: String,
        emoji: String,
        name: String,
        description: String,
        isSelected: Boolean,
        engineColor: Int
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.dpToPx(12),
                context.dpToPx(10),
                context.dpToPx(12),
                context.dpToPx(10)
            )
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )

            // Set background
            updateCardBackground(this, isSelected, engineColor)

            // Left color indicator
            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(4), context.dpToPx(40))
                background = createRoundedDrawable(
                    fillColor = engineColor,
                    cornerRadius = 2f
                )
            })

            // Spacing
            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(10), 1)
            })

            // Text container
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    text = "$emoji $name"
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 14f
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(context).apply {
                    text = description
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 11f
                })
            })

            // Switch
            addView(Switch(context).apply {
                this.isChecked = isSelected
                thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                trackTintList = android.content.res.ColorStateList.valueOf(engineColor)
                setOnCheckedChangeListener { _, isChecked ->
                    handleEngineSwitch(engineCode, isChecked)
                }

                // Store reference
                when (engineCode) {
                    AppConstants.STT_ENGINE_ANDROID -> androidSwitch = this
                    AppConstants.STT_ENGINE_GROQ -> groqSwitch = this
                    AppConstants.STT_ENGINE_GEMINI -> geminiSwitch = this
                }
            })
        }
    }

    /**
     * Handle engine switch toggle
     */
    private fun handleEngineSwitch(engineCode: String, isChecked: Boolean) {
        if (isChecked) {
            // Turn off other switches
            when (engineCode) {
                AppConstants.STT_ENGINE_ANDROID -> {
                    groqSwitch?.isChecked = false
                    geminiSwitch?.isChecked = false
                }
                AppConstants.STT_ENGINE_GROQ -> {
                    androidSwitch?.isChecked = false
                    geminiSwitch?.isChecked = false
                }
                AppConstants.STT_ENGINE_GEMINI -> {
                    androidSwitch?.isChecked = false
                    groqSwitch?.isChecked = false
                }
            }
            onEngineChange(engineCode)
        } else {
            // Re-check if trying to turn off (must have one selected)
            when (engineCode) {
                AppConstants.STT_ENGINE_ANDROID -> androidSwitch?.isChecked = true
                AppConstants.STT_ENGINE_GROQ -> groqSwitch?.isChecked = true
                AppConstants.STT_ENGINE_GEMINI -> geminiSwitch?.isChecked = true
            }
        }
    }

    /**
     * Update existing view elements
     */
    private fun updateExistingView(state: KeyboardUiState) {
        // Update active banner
        activeBanner?.background = createRoundedDrawable(
            fillColor = getEngineColorDark(state.sttEngine),
            cornerRadius = 8f
        )
        activeBannerIcon?.text = getEngineEmoji(state.sttEngine)
        activeBannerText?.text = if (state.isRecording) {
            "🔴 Recording with ${getEngineName(state.sttEngine)}..."
        } else {
            "${getEngineName(state.sttEngine)} - READY"
        }

        // Update engine cards
        updateCardBackground(androidCard, state.isUsingAndroidStt, ContextCompat.getColor(context, R.color.engine_android))
        updateCardBackground(groqCard, state.isUsingGroqStt, ContextCompat.getColor(context, R.color.engine_groq))
        updateCardBackground(geminiCard, state.isUsingGeminiStt, ContextCompat.getColor(context, R.color.engine_gemini))

        // Update switches (without triggering listeners)
        androidSwitch?.setOnCheckedChangeListener(null)
        groqSwitch?.setOnCheckedChangeListener(null)
        geminiSwitch?.setOnCheckedChangeListener(null)

        androidSwitch?.isChecked = state.isUsingAndroidStt
        groqSwitch?.isChecked = state.isUsingGroqStt
        geminiSwitch?.isChecked = state.isUsingGeminiStt

        androidSwitch?.setOnCheckedChangeListener { _, isChecked -> handleEngineSwitch(AppConstants.STT_ENGINE_ANDROID, isChecked) }
        groqSwitch?.setOnCheckedChangeListener { _, isChecked -> handleEngineSwitch(AppConstants.STT_ENGINE_GROQ, isChecked) }
        geminiSwitch?.setOnCheckedChangeListener { _, isChecked -> handleEngineSwitch(AppConstants.STT_ENGINE_GEMINI, isChecked) }

        // Update status text
        statusText?.text = getStatusMessage(state)

        // Update mic button
        updateMicButton(state)

        // Update result card visibility and content
        if (state.showResultCard && state.resultTextToShow != null) {
            resultText?.text = state.resultTextToShow
            resultCard?.visibility = View.VISIBLE
        } else {
            resultCard?.visibility = View.GONE
        }
    }

    /**
     * Update card background based on selection state
     */
    private fun updateCardBackground(card: LinearLayout?, isSelected: Boolean, engineColor: Int) {
        card?.apply {
            if (isSelected) {
                background = createRoundedDrawableWithBorder(
                    fillColor = ContextCompat.getColor(context, R.color.card_selected),
                    borderColor = engineColor,
                    borderWidth = 2f,
                    cornerRadius = 8f
                )
            } else {
                background = createRoundedDrawable(
                    fillColor = ContextCompat.getColor(context, R.color.card_background),
                    cornerRadius = 8f
                )
            }
        }
    }

    /**
     * Create the microphone button with engine indicator
     */
    private fun createMicButton(state: KeyboardUiState): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LayoutParams(
                context.dpToPx(80),
                context.dpToPx(90)
            ).apply {
                setMargins(0, context.dpToPx(8), 0, context.dpToPx(8))
            }

            // Engine color ring
            addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    context.dpToPx(72),
                    context.dpToPx(72),
                    Gravity.CENTER
                )
                background = createRoundedDrawable(
                    fillColor = Color.TRANSPARENT,
                    strokeColor = getEngineColor(state.sttEngine),
                    strokeWidth = 4f,
                    cornerRadius = 36f
                )
                micEngineIndicator = this
            })

            // Mic icon container
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    context.dpToPx(64),
                    context.dpToPx(64),
                    Gravity.CENTER
                )
                background = createRoundedDrawable(
                    fillColor = if (state.isRecording) {
                        ContextCompat.getColor(context, R.color.recording_active)
                    } else {
                        getEngineColor(state.sttEngine)
                    },
                    cornerRadius = 32f
                )

                micIcon = TextView(context).apply {
                    text = "🎤"
                    textSize = 28f
                    gravity = Gravity.CENTER
                }
                addView(micIcon)

                setOnClickListener {
                    if (ActivityCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        onMicClick()
                    } else {
                        statusText?.text = "❌ Microphone permission required"
                    }
                }
            })

            // Engine indicator dot
            addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    context.dpToPx(12),
                    context.dpToPx(12),
                    Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
                )
                background = createRoundedDrawable(
                    fillColor = getEngineColor(state.sttEngine),
                    cornerRadius = 6f
                )
            })
        }
    }

    /**
     * Update mic button appearance
     */
    private fun updateMicButton(state: KeyboardUiState) {
        micEngineIndicator?.background = createRoundedDrawable(
            fillColor = Color.TRANSPARENT,
            strokeColor = getEngineColor(state.sttEngine),
            strokeWidth = if (state.isRecording) 6f else 4f,
            cornerRadius = 36f
        )

        // Update button background
        (micButton?.getChildAt(1) as? LinearLayout)?.background = createRoundedDrawable(
            fillColor = if (state.isRecording) {
                ContextCompat.getColor(context, R.color.recording_active)
            } else {
                getEngineColor(state.sttEngine)
            },
            cornerRadius = 32f
        )
    }

    /**
     * Create language switcher chips
     */
    private fun createLanguageSwitcher(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, context.dpToPx(8))
            }

            addView(createChip(
                text = "English",
                isSelected = state.currentLanguage == Language.ENGLISH,
                onClick = { onLanguageChange(Language.ENGLISH) }
            ))
            addView(View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(12), 1)
            })
            addView(createChip(
                text = "বাংলা",
                isSelected = state.currentLanguage == Language.BENGALI,
                onClick = { onLanguageChange(Language.BENGALI) }
            ))
        }
    }

    /**
     * Create the status text view
     */
    private fun createStatusText(state: KeyboardUiState): TextView {
        return TextView(context).apply {
            text = getStatusMessage(state)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, context.dpToPx(4), 0, context.dpToPx(4))
        }
    }

    /**
     * Get status message based on state
     */
    private fun getStatusMessage(state: KeyboardUiState): String {
        return when {
            state.isRecording -> "🔴 Recording with ${getEngineName(state.sttEngine)}..."
            state.errorMessage != null -> "❌ ${state.errorMessage}"
            else -> "${getEngineEmoji(state.sttEngine)} ${getEngineName(state.sttEngine)} ready - Tap mic to speak"
        }
    }

    /**
     * Create the result card
     */
    private fun createResultCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            background = createRoundedDrawable(
                fillColor = ContextCompat.getColor(context, R.color.success_green),
                cornerRadius = 8f
            )
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
                setMargins(0, context.dpToPx(12), 0, 0)
            }
            visibility = View.GONE

            resultText = TextView(context).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 14f
            }
            addView(resultText)

            addView(Button(context).apply {
                text = "✅ Insert Text"
                setTextColor(ContextCompat.getColor(context, R.color.success_green))
                setBackgroundColor(Color.WHITE)
                textSize = 12f
                isAllCaps = false
                layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    context.dpToPx(36)
                ).apply {
                    setMargins(0, context.dpToPx(8), 0, 0)
                }
                setOnClickListener {
                    resultText?.text?.toString()?.let { text ->
                        if (text.isNotBlank()) {
                            onInsertText(text)
                            visibility = View.GONE
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
            textSize = 12f
            setTextColor(if (isSelected) Color.WHITE else ContextCompat.getColor(context, R.color.text_secondary))
            background = createRoundedDrawable(
                fillColor = if (isSelected) getEngineColor(AppConstants.STT_ENGINE_ANDROID) else ContextCompat.getColor(context, R.color.card_background),
                cornerRadius = 16f
            )
            setPadding(
                context.dpToPx(16),
                context.dpToPx(8),
                context.dpToPx(16),
                context.dpToPx(8)
            )
            isAllCaps = false
            setOnClickListener { onClick() }
        }
    }

    // ==================== Helper Methods ====================

    private fun getEngineName(engineCode: String): String {
        return when (engineCode) {
            AppConstants.STT_ENGINE_ANDROID -> "Android (Offline)"
            AppConstants.STT_ENGINE_GROQ -> "Groq (Online)"
            AppConstants.STT_ENGINE_GEMINI -> "Gemini (Live)"
            else -> "Unknown"
        }
    }

    private fun getEngineEmoji(engineCode: String): String {
        return when (engineCode) {
            AppConstants.STT_ENGINE_ANDROID -> "🟢"
            AppConstants.STT_ENGINE_GROQ -> "🔵"
            AppConstants.STT_ENGINE_GEMINI -> "🟣"
            else -> "⚪"
        }
    }

    private fun getEngineColor(engineCode: String): Int {
        return when (engineCode) {
            AppConstants.STT_ENGINE_ANDROID -> ContextCompat.getColor(context, R.color.engine_android)
            AppConstants.STT_ENGINE_GROQ -> ContextCompat.getColor(context, R.color.engine_groq)
            AppConstants.STT_ENGINE_GEMINI -> ContextCompat.getColor(context, R.color.engine_gemini)
            else -> Color.GRAY
        }
    }

    private fun getEngineColorDark(engineCode: String): Int {
        return when (engineCode) {
            AppConstants.STT_ENGINE_ANDROID -> ContextCompat.getColor(context, R.color.engine_android_dark)
            AppConstants.STT_ENGINE_GROQ -> ContextCompat.getColor(context, R.color.engine_groq_dark)
            AppConstants.STT_ENGINE_GEMINI -> ContextCompat.getColor(context, R.color.engine_gemini_dark)
            else -> Color.GRAY
        }
    }

    private fun createRoundedDrawable(
        fillColor: Int,
        strokeColor: Int = Color.TRANSPARENT,
        strokeWidth: Float = 0f,
        cornerRadius: Float = 8f
    ): GradientDrawable {
        return GradientDrawable().apply {
            this.setColor(fillColor)
            this.cornerRadius = context.dpToPx(cornerRadius.toInt()).toFloat()
            if (strokeWidth > 0 && strokeColor != Color.TRANSPARENT) {
                this.setStroke(context.dpToPx(strokeWidth.toInt()), strokeColor)
            }
        }
    }

    private fun createRoundedDrawableWithBorder(
        fillColor: Int,
        borderColor: Int,
        borderWidth: Float,
        cornerRadius: Float
    ): GradientDrawable {
        return GradientDrawable().apply {
            this.setColor(fillColor)
            this.cornerRadius = context.dpToPx(cornerRadius.toInt()).toFloat()
            this.setStroke(context.dpToPx(borderWidth.toInt()), borderColor)
        }
    }

    companion object {
        fun create(
            context: Context,
            onMicClick: () -> Unit,
            onEngineChange: (String) -> Unit,
            onLanguageChange: (Language) -> Unit,
            onInsertText: (String) -> Unit
        ): VoiceInputView {
            return VoiceInputView(
                context,
                onMicClick,
                onEngineChange,
                onLanguageChange,
                onInsertText
            )
        }
    }
}
