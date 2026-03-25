package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.KeyboardTheme

class AiKeyboardService : InputMethodService() {
    
    companion object {
        private const val TAG = "AiKeyboardService"
    }
    
    private var composeView: ComposeView? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Keyboard service created ===")
    }
    
    override fun onCreateInputView(): View {
        Log.d(TAG, "=== Creating input view ===")
        
        return try {
            composeView = ComposeView(this).apply {
                setContent {
                    KeyboardTheme {
                        KeyboardContent(
                            onTextCommit = { text ->
                                try {
                                    currentInputConnection?.commitText(text, 1)
                                } catch (e: Exception) {
                                    Log.e(TAG, "commitText error: ${e.message}")
                                }
                            },
                            onDelete = {
                                try {
                                    currentInputConnection?.deleteSurroundingText(1, 0)
                                } catch (e: Exception) {
                                    Log.e(TAG, "deleteSurroundingText error: ${e.message}")
                                }
                            },
                            onEnter = {
                                try {
                                    currentInputConnection?.performEditorAction(EditorInfo.IME_ACTION_DONE)
                                } catch (e: Exception) {
                                    Log.e(TAG, "performEditorAction error: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }
            Log.d(TAG, "=== ComposeView created successfully ===")
            composeView!!
        } catch (e: Exception) {
            Log.e(TAG, "=== ERROR creating ComposeView: ${e.message}", e)
            // Return an empty view as fallback
            View(this)
        }
    }
    
    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        Log.d(TAG, "onStartInput: restarting=$restarting")
    }
    
    override fun onStartInputView(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(editorInfo, restarting)
        Log.d(TAG, "onStartInputView: restarting=$restarting")
    }
    
    override fun onFinishInput() {
        super.onFinishInput()
        Log.d(TAG, "onFinishInput")
    }
    
    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        Log.d(TAG, "onFinishInputView: finishing=$finishingInput")
    }
    
    override fun onDestroy() {
        Log.d(TAG, "=== onDestroy ===")
        try {
            composeView?.disposeComposition()
        } catch (e: Exception) {
            Log.e(TAG, "disposeComposition error: ${e.message}")
        }
        composeView = null
        super.onDestroy()
    }
}
