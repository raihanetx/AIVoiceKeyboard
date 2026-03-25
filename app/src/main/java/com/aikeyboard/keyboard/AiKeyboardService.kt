package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.AiKeyboardApp

class AiKeyboardService : InputMethodService() {

    companion object {
        private const val TAG = "AiKeyboard"
        private var instance: AiKeyboardService? = null

        fun getInstance(): AiKeyboardService? = instance
    }

    private var composeView: ComposeView? = null
    private var currentText = StringBuilder()

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "=== KEYBOARD SERVICE CREATED ===")
        Log.d(TAG, "Package: ${packageName}")
    }

    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating keyboard view with Compose ===")

        return try {
            composeView = ComposeView(this).apply {
                setContent {
                    KeyboardTheme {
                        KeyboardContent(
                            onTextCommit = { text ->
                                commitText(text)
                            },
                            onDelete = {
                                performDelete()
                            },
                            onEnter = {
                                performEnter()
                            }
                        )
                    }
                }
            }
            Log.d(TAG, "=== Compose keyboard view created ===")
            composeView!!
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Compose view", e)
            createFallbackView()
        }
    }

    private fun createFallbackView(): View {
        Log.d(TAG, "Creating fallback view")
        return android.widget.TextView(this).apply {
            text = "AI Voice Keyboard - Error loading"
            setPadding(32, 32, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#1A1A2E"))
            setTextColor(android.graphics.Color.WHITE)
            textSize = 18f
            gravity = android.view.Gravity.CENTER
        }
    }

    private fun commitText(text: String) {
        currentText.append(text)
        currentInputConnection?.commitText(text, 1)
        Log.d(TAG, "Committed: '$text', Total: '${currentText}'")
    }

    private fun performDelete() {
        if (currentText.isNotEmpty()) {
            currentText.deleteCharAt(currentText.length - 1)
        }
        currentInputConnection?.deleteSurroundingText(1, 0)
        Log.d(TAG, "Delete, Remaining: '${currentText}'")
    }

    private fun performEnter() {
        currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
        Log.d(TAG, "Enter pressed")
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput: restarting=$restarting")
    }

    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "=== KEYBOARD VISIBLE - READY TO TYPE ===")
    }

    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView")
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        Log.d(TAG, "Keyboard window hidden")
    }

    override fun onWindowShown() {
        super.onWindowShown()
        Log.d(TAG, "Keyboard window shown")
    }

    override fun onDestroy() {
        Log.d(TAG, "=== SERVICE DESTROYED ===")
        instance = null
        composeView = null
        super.onDestroy()
    }
}
