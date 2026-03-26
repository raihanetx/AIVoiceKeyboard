package com.aikeyboard.presentation.keyboard.view

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.core.extension.dpToPx
import com.aikeyboard.presentation.keyboard.ApiKeyStatus

/**
 * Engine Card View - A selectable card for an STT engine
 */
class EngineCardView(
    context: Context,
    private val engineCode: String,
    private val engineName: String,
    private val engineEmoji: String,
    private val description: String,
    private val engineColor: Int,
    private val needsApiKey: Boolean
) : LinearLayout(context) {

    // Callbacks
    var onSelected: (() -> Unit)? = null
    var onApiKeyEntered: ((String) -> Unit)? = null

    // UI Components
    private val container: LinearLayout
    private val radioCircle: View
    private val statusBadge: TextView
    private val apiKeyContainer: LinearLayout
    private val apiKeyInput: EditText
    private val saveBtn: Button

    // State
    private var isSelectedEngine: Boolean = false
    private var currentApiKeyStatus: ApiKeyStatus = ApiKeyStatus.NONE

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, 0, 0, context.dpToPx(6))
        }

        // Main container - clickable card
        container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12)
            )
            background = createCardBackground(false)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            isClickable = true
            isFocusable = true

            // Radio circle indicator
            radioCircle = View(context).apply {
                layoutParams = LayoutParams(context.dpToPx(22), context.dpToPx(22)).apply {
                    setMargins(0, 0, context.dpToPx(12), 0)
                }
                background = createRadioBackground(false)
            }
            addView(radioCircle)

            // Text content
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)

                // Title row with badge
                addView(LinearLayout(context).apply {
                    orientation = HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

                    addView(TextView(context).apply {
                        text = "$engineEmoji $engineName"
                        setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                        textSize = 15f
                        typeface = Typeface.DEFAULT_BOLD
                    })

                    // Status badge
                    statusBadge = TextView(context).apply {
                        text = ""
                        textSize = 9f
                        setPadding(context.dpToPx(6), context.dpToPx(2), context.dpToPx(6), context.dpToPx(2))
                        visibility = if (needsApiKey) View.VISIBLE else View.GONE
                        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                            setMargins(context.dpToPx(8), 0, 0, 0)
                        }
                    }
                    addView(statusBadge)
                })

                // Description
                addView(TextView(context).apply {
                    text = description
                    setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                    textSize = 12f
                })
            })

            setOnClickListener {
                onSelected?.invoke()
            }
        }
        addView(container)

        // API Key input container
        apiKeyContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            visibility = View.GONE
            setPadding(context.dpToPx(44), context.dpToPx(4), context.dpToPx(12), context.dpToPx(12))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            // Input row
            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

                // API key input
                apiKeyInput = EditText(context).apply {
                    hint = "Paste your API key"
                    textSize = 13f
                    setSingleLine()
                    imeOptions = EditorInfo.IME_ACTION_DONE
                    setPadding(context.dpToPx(12), context.dpToPx(10), context.dpToPx(12), context.dpToPx(10))
                    background = createInputBackground()
                    layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                        setMargins(0, 0, context.dpToPx(8), 0)
                    }
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            val key = text?.toString()?.trim() ?: ""
                            if (key.isNotEmpty()) {
                                onApiKeyEntered?.invoke(key)
                            }
                            true
                        } else {
                            false
                        }
                    }
                }
                addView(apiKeyInput)

                // Save button
                saveBtn = Button(context).apply {
                    text = "Save"
                    textSize = 12f
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setPadding(context.dpToPx(14), context.dpToPx(8), context.dpToPx(14), context.dpToPx(8))
                    background = createButtonBackground(engineColor)
                    setOnClickListener {
                        val key = apiKeyInput.text?.toString()?.trim() ?: ""
                        if (key.isNotEmpty()) {
                            onApiKeyEntered?.invoke(key)
                        } else {
                            apiKeyInput.error = "Enter API key"
                        }
                    }
                }
                addView(saveBtn)
            })

            // Help text
            addView(TextView(context).apply {
                text = when (engineCode) {
                    "groq" -> "Get key: console.groq.com/keys"
                    "gemini" -> "Get key: aistudio.google.com/apikey"
                    else -> ""
                }
                setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                textSize = 10f
                setPadding(0, context.dpToPx(4), 0, 0)
            })
        }
        addView(apiKeyContainer)

        // Initial badge update
        updateStatusBadge()
    }

    fun setEngineSelected(selected: Boolean) {
        isSelectedEngine = selected
        container.background = createCardBackground(selected)
        radioCircle.background = createRadioBackground(selected)
        updateStatusBadge()
    }

    fun setApiKeyStatus(status: ApiKeyStatus) {
        currentApiKeyStatus = status
        updateStatusBadge()
        if (status == ApiKeyStatus.SAVED) {
            hideApiKeyInput()
        }
    }

    fun showApiKeyInput() {
        apiKeyContainer.visibility = View.VISIBLE
        apiKeyInput.setText("")
        apiKeyInput.requestFocus()
    }

    fun hideApiKeyInput() {
        apiKeyContainer.visibility = View.GONE
    }

    private fun updateStatusBadge() {
        if (!needsApiKey) {
            statusBadge.visibility = View.GONE
            return
        }

        statusBadge.visibility = View.VISIBLE
        when (currentApiKeyStatus) {
            ApiKeyStatus.SAVED -> {
                statusBadge.text = "✓ Ready"
                statusBadge.setTextColor(Color.parseColor("#4CAF50"))
                statusBadge.background = createBadgeBackground(Color.parseColor("#1B5E20"))
            }
            ApiKeyStatus.NONE -> {
                statusBadge.text = "Key Required"
                statusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_hint))
                statusBadge.background = createBadgeBackground(Color.parseColor("#333333"))
            }
        }
    }

    private fun createCardBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            if (selected) {
                setColor(ContextCompat.getColor(context, R.color.card_selected))
                setStroke(context.dpToPx(3), engineColor)
            } else {
                setColor(ContextCompat.getColor(context, R.color.card_background))
                setStroke(context.dpToPx(1), Color.parseColor("#444444"))
            }
            cornerRadius = context.dpToPx(12).toFloat()
        }
    }

    private fun createRadioBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            if (selected) {
                setColor(engineColor)
            } else {
                setColor(Color.parseColor("#333333"))
                setStroke(context.dpToPx(2), Color.parseColor("#555555"))
            }
        }
    }

    private fun createBadgeBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.dpToPx(4).toFloat()
        }
    }

    private fun createInputBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#1A1A2E"))
            setStroke(context.dpToPx(1), Color.parseColor("#555555"))
            cornerRadius = context.dpToPx(6).toFloat()
        }
    }

    private fun createButtonBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = context.dpToPx(6).toFloat()
        }
    }
}
