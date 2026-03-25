package com.aikeyboard.keyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.platform.ComposeView
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme

class AiKeyboardService : InputMethodService() {
    override fun onCreateInputView(): View {
        return ComposeView(this).apply {
            setContent {
                AIVoiceKeyboardTheme {
                    KeyboardContent()
                }
            }
        }
    }
}
