package com.aikeyboard.presentation.keyboard.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.aikeyboard.R
import com.aikeyboard.core.extension.dpToPx

/**
 * Error Display View - Shows detailed error messages with copy functionality
 */
class ErrorDisplayView(context: Context) : LinearLayout(context) {

    private val errorTitle: TextView
    private val errorMessage: TextView
    private val errorDetails: TextView
    private val copyButton: Button
    private val container: LinearLayout

    private var currentError: ErrorInfo? = null

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            setMargins(0, context.dpToPx(8), 0, 0)
        }

        container = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12),
                context.dpToPx(12)
            )
            background = createErrorBackground()
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

            // Error title with icon
            errorTitle = TextView(context).apply {
                text = "❌ Error"
                setTextColor(Color.parseColor("#FF5252"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(errorTitle)

            // Main error message
            errorMessage = TextView(context).apply {
                text = ""
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(0, context.dpToPx(8), 0, 0)
            }
            addView(errorMessage)

            // Technical details (collapsible)
            errorDetails = TextView(context).apply {
                text = ""
                setTextColor(Color.parseColor("#B0BEC5"))
                textSize = 11f
                typeface = Typeface.MONOSPACE
                setPadding(0, context.dpToPx(6), 0, 0)
                visibility = View.GONE
            }
            addView(errorDetails)

            // Button row
            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.END
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, context.dpToPx(10), 0, 0)
                }

                // Show details button
                addView(Button(context).apply {
                    text = "Details"
                    textSize = 11f
                    isAllCaps = false
                    setTextColor(Color.parseColor("#B0BEC5"))
                    setBackgroundColor(Color.TRANSPARENT)
                    setPadding(context.dpToPx(8), context.dpToPx(4), context.dpToPx(8), context.dpToPx(4))
                    setOnClickListener {
                        if (errorDetails.visibility == View.VISIBLE) {
                            errorDetails.visibility = View.GONE
                            text = "Details"
                        } else {
                            errorDetails.visibility = View.VISIBLE
                            text = "Hide"
                        }
                    }
                })

                // Copy button
                copyButton = Button(context).apply {
                    text = "📋 Copy Error"
                    textSize = 11f
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setPadding(context.dpToPx(12), context.dpToPx(6), context.dpToPx(12), context.dpToPx(6))
                    background = createCopyButtonBackground()
                    setOnClickListener { copyErrorToClipboard() }
                }
                addView(copyButton)
            })
        }
        addView(container)

        visibility = View.GONE
    }

    /**
     * Show an error message
     */
    fun showError(title: String, message: String, details: String? = null) {
        currentError = ErrorInfo(title, message, details)

        errorTitle.text = "❌ $title"
        errorMessage.text = message
        errorDetails.text = details ?: ""

        visibility = View.VISIBLE
        errorDetails.visibility = View.GONE
    }

    /**
     * Show API error with full details
     */
    fun showApiError(engine: String, errorType: String, message: String, fullError: String? = null) {
        val title = "$engine Error"
        val displayMessage = when (errorType) {
            "auth" -> "🔐 Authentication failed. Please check your API key."
            "network" -> "🌐 Network error. Please check your internet connection."
            "timeout" -> "⏱️ Request timed out. Please try again."
            "invalid_key" -> "🔑 Invalid API key. Please verify your key is correct."
            "rate_limit" -> "🚫 Rate limit exceeded. Please wait and try again."
            "server" -> "⚠️ Server error. Please try again later."
            "no_audio" -> "🔇 No audio was recorded. Please try again."
            "empty_result" -> "📝 No speech detected. Please speak clearly."
            else -> message
        }

        showError(title, displayMessage, fullError)
    }

    /**
     * Hide the error display
     */
    fun hideError() {
        visibility = View.GONE
        currentError = null
    }

    /**
     * Copy error to clipboard
     */
    private fun copyErrorToClipboard() {
        val error = currentError ?: return

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Error Message", """
=== AI Voice Keyboard Error ===
Title: ${error.title}
Message: ${error.message}
Details: ${error.details ?: "None"}
Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
===============================
        """.trimIndent())
        clipboard.setPrimaryClip(clip)

        Toast.makeText(context, "Error copied to clipboard!", Toast.LENGTH_SHORT).show()
    }

    private fun createErrorBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#3D1F1F"))
            setStroke(context.dpToPx(1), Color.parseColor("#FF5252"))
            cornerRadius = context.dpToPx(8).toFloat()
        }
    }

    private fun createCopyButtonBackground(): GradientDrawable {
        return GradientDrawable().apply {
            setColor(Color.parseColor("#FF5252"))
            cornerRadius = context.dpToPx(4).toFloat()
        }
    }

    /**
     * Data class for error information
     */
    data class ErrorInfo(
        val title: String,
        val message: String,
        val details: String?
    )

    companion object {
        fun create(context: Context): ErrorDisplayView {
            return ErrorDisplayView(context)
        }
    }
}
