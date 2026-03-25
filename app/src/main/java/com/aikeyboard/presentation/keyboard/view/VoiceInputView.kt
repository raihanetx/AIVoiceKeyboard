package com.aikeyboard.presentation.keyboard.view

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
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

    private var statusText: TextView? = null
    private var resultText: TextView? = null
    private var resultCard: LinearLayout? = null
    
    // Toggle buttons for engines
    private var androidToggle: ToggleButton? = null
    private var groqToggle: ToggleButton? = null
    private var geminiToggle: ToggleButton? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#1A1A2E"))
        setPadding(
            context.dpToPx(16),
            context.dpToPx(16),
            context.dpToPx(16),
            context.dpToPx(16)
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
        
        // Engine switcher with toggles
        addView(createEngineToggleSection(state))

        // Language switcher
        addView(createLanguageSwitcher(state))

        // Mic button
        addView(createMicButton(state))

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
            text = "Voice Recognition"
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(12))
        }
    }

    /**
     * Create engine toggle section
     */
    private fun createEngineToggleSection(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.parseColor("#252540"))
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
            
            // Engine selection label
            addView(TextView(context).apply {
                text = "Select STT Engine:"
                setTextColor(Color.GRAY)
                textSize = 11f
                setPadding(0, 0, 0, context.dpToPx(8))
            })
            
            // Toggle buttons container
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                
                // Android Offline toggle
                androidToggle = createEngineToggle(
                    "Android (Offline)",
                    "Works without internet",
                    state.isUsingAndroidStt,
                    AppConstants.STT_ENGINE_ANDROID
                )
                addView(androidToggle)
                
                // Groq Online toggle
                groqToggle = createEngineToggle(
                    "Groq (Online)",
                    "Fast & accurate, ~10K req/day free",
                    state.isUsingGroqStt,
                    AppConstants.STT_ENGINE_GROQ
                )
                addView(groqToggle)
                
                // Gemini Live toggle
                geminiToggle = createEngineToggle(
                    "Gemini (Live)",
                    "Best quality, multimodal AI",
                    state.isUsingGeminiStt,
                    AppConstants.STT_ENGINE_GEMINI
                )
                addView(geminiToggle)
            })
        }
    }
    
    /**
     * Create an engine toggle button
     */
    private fun createEngineToggle(
        name: String,
        description: String,
        isChecked: Boolean,
        engineCode: String
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, context.dpToPx(4), 0, context.dpToPx(4))
            
            // Toggle switch
            addView(ToggleButton(context).apply {
                textOn = ""
                textOff = ""
                text = null
                this.isChecked = isChecked
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LayoutParams(
                    context.dpToPx(48),
                    context.dpToPx(24)
                )
                setOnClickListener {
                    // Uncheck other toggles
                    when (engineCode) {
                        AppConstants.STT_ENGINE_ANDROID -> {
                            groqToggle?.isChecked = false
                            geminiToggle?.isChecked = false
                        }
                        AppConstants.STT_ENGINE_GROQ -> {
                            androidToggle?.isChecked = false
                            geminiToggle?.isChecked = false
                        }
                        AppConstants.STT_ENGINE_GEMINI -> {
                            androidToggle?.isChecked = false
                            groqToggle?.isChecked = false
                        }
                    }
                    // Keep this toggle checked
                    this.isChecked = true
                    onEngineChange(engineCode)
                }
            })
            
            // Text container
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                setPadding(context.dpToPx(8), 0, 0, 0)
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
                
                addView(TextView(context).apply {
                    text = name
                    setTextColor(Color.WHITE)
                    textSize = 13f
                })
                addView(TextView(context).apply {
                    text = description
                    setTextColor(Color.GRAY)
                    textSize = 10f
                })
            })
        }
    }

    /**
     * Update existing view elements
     */
    private fun updateExistingView(state: KeyboardUiState) {
        statusText?.text = state.voiceStatusText

        // Update toggle states
        androidToggle?.isChecked = state.isUsingAndroidStt
        groqToggle?.isChecked = state.isUsingGroqStt
        geminiToggle?.isChecked = state.isUsingGeminiStt

        // Update result card visibility and content
        if (state.showResultCard && state.resultTextToShow != null) {
            resultText?.text = state.resultTextToShow
            resultCard?.visibility = View.VISIBLE
        } else {
            resultCard?.visibility = View.GONE
        }
    }

    /**
     * Create language switcher chips
     */
    private fun createLanguageSwitcher(state: KeyboardUiState): LinearLayout {
        return LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, context.dpToPx(12))

            addView(createChip(
                text = "English",
                isSelected = state.currentLanguage == Language.ENGLISH,
                onClick = { onLanguageChange(Language.ENGLISH) }
            ))
            addView(View(context).apply { 
                layoutParams = LayoutParams(context.dpToPx(16), 1) 
            })
            addView(createChip(
                text = "বাংলা",
                isSelected = state.currentLanguage == Language.BENGALI,
                onClick = { onLanguageChange(Language.BENGALI) }
            ))
        }
    }

    /**
     * Create the microphone button
     */
    private fun createMicButton(state: KeyboardUiState): Button {
        return Button(context).apply {
            text = "🎤"
            textSize = 32f
            setBackgroundColor(
                if (state.isRecording) Color.parseColor("#FF5722") 
                else Color.parseColor("#4285F4")
            )
            setTextColor(Color.WHITE)
            layoutParams = LayoutParams(
                context.dpToPx(AppConstants.MIC_BUTTON_SIZE_DP),
                context.dpToPx(AppConstants.MIC_BUTTON_SIZE_DP)
            ).apply {
                setMargins(0, context.dpToPx(8), 0, context.dpToPx(8))
            }
            setOnClickListener {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    onMicClick()
                } else {
                    statusText?.text = "Microphone permission required"
                }
            }
        }
    }

    /**
     * Create the status text view
     */
    private fun createStatusText(state: KeyboardUiState): TextView {
        return TextView(context).apply {
            text = state.voiceStatusText
            setTextColor(Color.GRAY)
            textSize = 12f
            gravity = Gravity.CENTER
        }
    }

    /**
     * Create the result card
     */
    private fun createResultCard(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundColor(Color.parseColor("#1B5E20"))
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
                setMargins(0, context.dpToPx(16), 0, 0)
            }
            visibility = View.GONE

            resultText = TextView(context).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 14f
            }
            addView(resultText)

            addView(Button(context).apply {
                text = "Insert Text"
                setTextColor(Color.parseColor("#1B5E20"))
                setBackgroundColor(Color.WHITE)
                textSize = 12f
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
         * Create a voice input view
         */
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
