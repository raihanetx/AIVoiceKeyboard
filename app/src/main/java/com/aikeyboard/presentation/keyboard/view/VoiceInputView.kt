package com.aikeyboard.presentation.keyboard.view

import android.Manifest
import android.app.AlertDialog
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
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.keyboard.KeyboardUiState

/**
 * Voice input view for speech-to-text
 * Simple and functional - 3 engines with radio selection
 */
class VoiceInputView(
    context: Context,
    private val onMicClick: () -> Unit,
    private val onEngineChange: (String) -> Unit,
    private val onLanguageChange: (Language) -> Unit,
    private val onInsertText: (String) -> Unit,
    private val onOpenSettings: () -> Unit
) : LinearLayout(context) {

    // UI Components
    private var statusText: TextView? = null
    private var resultText: TextView? = null
    private var resultCard: LinearLayout? = null
    private var micButton: FrameLayout? = null

    // Engine radio buttons
    private var androidRadio: RadioButton? = null
    private var groqRadio: RadioButton? = null
    private var geminiRadio: RadioButton? = null

    // API Key status indicators
    private var groqKeyStatus: TextView? = null
    private var geminiKeyStatus: TextView? = null

    // Current state
    private var currentState: KeyboardUiState? = null

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

    fun updateState(state: KeyboardUiState) {
        currentState = state
        if (childCount == 0) {
            buildView(state)
        } else {
            updateExistingView(state)
        }
    }

    private fun buildView(state: KeyboardUiState) {
        // Title
        addView(createTitle())

        // Engine selection section
        addView(createEngineSection(state))

        // Language switcher
        addView(createLanguageSwitcher(state))

        // Mic button
        micButton = createMicButton(state)
        addView(micButton)

        // Status text
        statusText = createStatusText(state)
        addView(statusText)

        // Result card
        resultCard = createResultCard()
        addView(resultCard)
    }

    private fun createTitle(): TextView {
        return TextView(context).apply {
            text = "🎤 Voice Typing"
            setTextColor(ContextCompat.getColor(context, R.color.text_primary))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(12))
        }
    }

    private fun createEngineSection(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.card_background))
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
                setMargins(0, 0, 0, context.dpToPx(12))
            }

            // Section title
            addView(TextView(context).apply {
                text = "Select Engine:"
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                textSize = 12f
                setPadding(0, 0, 0, context.dpToPx(8))
            })

            // RadioGroup for engines
            addView(RadioGroup(context).apply {
                orientation = RadioGroup.VERTICAL
                id = View.generateViewId()

                // Android Offline option
                addView(createEngineRadio(
                    text = "🟢 Android (Offline)",
                    description = "Free, no internet needed",
                    isSelected = state.isUsingAndroidStt,
                    engineCode = AppConstants.STT_ENGINE_ANDROID
                ) { androidRadio = it })

                // Groq Online option
                addView(createEngineRadioWithStatus(
                    text = "🔵 Groq (Online)",
                    description = "Fast & accurate",
                    isSelected = state.isUsingGroqStt,
                    engineCode = AppConstants.STT_ENGINE_GROQ,
                    needsApiKey = true
                ) { radio, status ->
                    groqRadio = radio
                    groqKeyStatus = status
                })

                // Gemini Live option
                addView(createEngineRadioWithStatus(
                    text = "🟣 Gemini (Live)",
                    description = "Best quality streaming",
                    isSelected = state.isUsingGeminiStt,
                    engineCode = AppConstants.STT_ENGINE_GEMINI,
                    needsApiKey = true
                ) { radio, status ->
                    geminiRadio = radio
                    geminiKeyStatus = status
                })

                setOnCheckedChangeListener { _, checkedId ->
                    handleEngineSelection(checkedId)
                }
            })
        }
    }

    private fun createEngineRadio(
        text: String,
        description: String,
        isSelected: Boolean,
        engineCode: String,
        onCreated: (RadioButton) -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dpToPx(4), 0, context.dpToPx(4))

            val radioButton = RadioButton(context).apply {
                id = View.generateViewId()
                this.isChecked = isSelected
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                textSize = 14f
                tag = engineCode
            }
            onCreated(radioButton)
            addView(radioButton)

            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(context.dpToPx(8), 0, 0, 0)
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                addView(TextView(context).apply {
                    this.text = text
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 14f
                })
                addView(TextView(context).apply {
                    this.text = description
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 11f
                })
            })
        }
    }

    private fun createEngineRadioWithStatus(
        text: String,
        description: String,
        isSelected: Boolean,
        engineCode: String,
        needsApiKey: Boolean,
        onCreated: (RadioButton, TextView) -> Unit
    ): LinearLayout {
        var statusTextView: TextView? = null

        return LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(0, context.dpToPx(4), 0, context.dpToPx(4))

            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                val radioButton = RadioButton(context).apply {
                    id = View.generateViewId()
                    this.isChecked = isSelected
                    setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                    textSize = 14f
                    tag = engineCode
                }

                addView(radioButton)

                addView(LinearLayout(context).apply {
                    orientation = VERTICAL
                    setPadding(context.dpToPx(8), 0, 0, 0)
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                    addView(TextView(context).apply {
                        this.text = text
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        textSize = 14f
                    })
                    addView(TextView(context).apply {
                        this.text = description
                        setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                        textSize = 11f
                    })
                })

                // API Key status
                statusTextView = TextView(context).apply {
                    text = "❌ No Key"
                    setTextColor(Color.parseColor("#FF5252"))
                    textSize = 10f
                    setPadding(context.dpToPx(4), context.dpToPx(2), context.dpToPx(4), context.dpToPx(2))
                    background = createRoundedDrawable(
                        fillColor = Color.parseColor("#33FF5252"),
                        cornerRadius = 4f
                    )
                }
                addView(statusTextView)

                onCreated(radioButton, statusTextView!!)
            })

            // Add "Set Key" button
            addView(Button(context).apply {
                text = "Set API Key"
                textSize = 11f
                setTextColor(ContextCompat.getColor(context, R.color.engine_groq))
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 0, 0, 0)
                isAllCaps = false
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(context.dpToPx(40), 0, 0, 0)
                }
                setOnClickListener {
                    onOpenSettings()
                }
            })
        }
    }

    private fun handleEngineSelection(checkedId: Int) {
        // Find which radio button was selected
        val androidId = androidRadio?.id ?: -1
        val groqId = groqRadio?.id ?: -1
        val geminiId = geminiRadio?.id ?: -1

        when (checkedId) {
            androidId -> onEngineChange(AppConstants.STT_ENGINE_ANDROID)
            groqId -> onEngineChange(AppConstants.STT_ENGINE_GROQ)
            geminiId -> onEngineChange(AppConstants.STT_ENGINE_GEMINI)
        }
    }

    private fun updateExistingView(state: KeyboardUiState) {
        // Update radio buttons (without triggering listener)
        val radioGroup = (getChildAt(1) as? LinearLayout)?.getChildAt(1) as? RadioGroup

        radioGroup?.setOnCheckedChangeListener(null)

        androidRadio?.isChecked = state.isUsingAndroidStt
        groqRadio?.isChecked = state.isUsingGroqStt
        geminiRadio?.isChecked = state.isUsingGeminiStt

        radioGroup?.setOnCheckedChangeListener { _, checkedId ->
            handleEngineSelection(checkedId)
        }

        // Update status text
        statusText?.text = getStatusMessage(state)

        // Update mic button color
        updateMicButton(state)

        // Update result card
        if (state.showResultCard && state.resultTextToShow != null) {
            resultText?.text = state.resultTextToShow
            resultCard?.visibility = View.VISIBLE
        } else {
            resultCard?.visibility = View.GONE
        }
    }

    private fun createMicButton(state: KeyboardUiState): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LayoutParams(
                context.dpToPx(80),
                context.dpToPx(80)
            ).apply {
                setMargins(0, context.dpToPx(12), 0, context.dpToPx(12))
            }

            // Background circle with engine color
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    context.dpToPx(72),
                    context.dpToPx(72),
                    Gravity.CENTER
                )
                background = createRoundedDrawable(
                    fillColor = if (state.isRecording) {
                        ContextCompat.getColor(context, R.color.recording_active)
                    } else {
                        getEngineColor(state.sttEngine)
                    },
                    cornerRadius = 36f
                )

                addView(TextView(context).apply {
                    text = "🎤"
                    textSize = 32f
                    gravity = Gravity.CENTER
                })

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
        }
    }

    private fun updateMicButton(state: KeyboardUiState) {
        (micButton?.getChildAt(0) as? LinearLayout)?.background = createRoundedDrawable(
            fillColor = if (state.isRecording) {
                ContextCompat.getColor(context, R.color.recording_active)
            } else {
                getEngineColor(state.sttEngine)
            },
            cornerRadius = 36f
        )
    }

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

    private fun createStatusText(state: KeyboardUiState): TextView {
        return TextView(context).apply {
            text = getStatusMessage(state)
            setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            textSize = 12f
            gravity = Gravity.CENTER
        }
    }

    private fun getStatusMessage(state: KeyboardUiState): String {
        return when {
            state.isRecording -> "🔴 Recording with ${getEngineName(state.sttEngine)}..."
            state.errorMessage != null -> "❌ ${state.errorMessage}"
            else -> "${getEngineEmoji(state.sttEngine)} ${getEngineName(state.sttEngine)} ready - Tap mic"
        }
    }

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
                setMargins(0, context.dpToPx(8), 0, 0)
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
                fillColor = if (isSelected) ContextCompat.getColor(context, R.color.primary) else ContextCompat.getColor(context, R.color.card_background),
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

    // Helpers
    private fun getEngineName(engineCode: String): String {
        return when (engineCode) {
            AppConstants.STT_ENGINE_ANDROID -> "Android"
            AppConstants.STT_ENGINE_GROQ -> "Groq"
            AppConstants.STT_ENGINE_GEMINI -> "Gemini"
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

    private fun createRoundedDrawable(
        fillColor: Int,
        cornerRadius: Float = 8f
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fillColor)
            this.cornerRadius = context.dpToPx(cornerRadius.toInt()).toFloat()
        }
    }

    companion object {
        fun create(
            context: Context,
            onMicClick: () -> Unit,
            onEngineChange: (String) -> Unit,
            onLanguageChange: (Language) -> Unit,
            onInsertText: (String) -> Unit,
            onOpenSettings: () -> Unit = {}
        ): VoiceInputView {
            return VoiceInputView(
                context,
                onMicClick,
                onEngineChange,
                onLanguageChange,
                onInsertText,
                onOpenSettings
            )
        }
    }
}
