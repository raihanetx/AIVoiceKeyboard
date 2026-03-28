package com.aikeyboard.service

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import com.aikeyboard.R

class AiKeyboardService : InputMethodService() {

    private var isCaps = false

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        setupKeys(view)
        return view
    }

    private fun setupKeys(view: View) {
        // Letter keys
        val letterIds = listOf(
            R.id.key_q, R.id.key_w, R.id.key_e, R.id.key_r, R.id.key_t,
            R.id.key_y, R.id.key_u, R.id.key_i, R.id.key_o, R.id.key_p,
            R.id.key_a, R.id.key_s, R.id.key_d, R.id.key_f, R.id.key_g,
            R.id.key_h, R.id.key_j, R.id.key_k, R.id.key_l,
            R.id.key_z, R.id.key_x, R.id.key_c, R.id.key_v, R.id.key_b,
            R.id.key_n, R.id.key_m
        )

        for (id in letterIds) {
            view.findViewById<Button>(id)?.setOnClickListener { v ->
                val btn = v as Button
                val text = if (isCaps) btn.text.toString() else btn.text.toString().lowercase()
                sendText(text)
            }
        }

        // Special keys
        view.findViewById<Button>(R.id.key_shift)?.setOnClickListener {
            isCaps = !isCaps
            Toast.makeText(this, if (isCaps) "CAPS ON" else "caps off", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.key_backspace)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)
            )
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL)
            )
        }

        view.findViewById<Button>(R.id.key_space)?.setOnClickListener {
            sendText(" ")
        }

        view.findViewById<Button>(R.id.key_comma)?.setOnClickListener {
            sendText(",")
        }

        view.findViewById<Button>(R.id.key_dot)?.setOnClickListener {
            sendText(".")
        }

        view.findViewById<Button>(R.id.key_enter)?.setOnClickListener {
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)
            )
            currentInputConnection?.sendKeyEvent(
                KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER)
            )
        }

        view.findViewById<Button>(R.id.key_numbers)?.setOnClickListener {
            Toast.makeText(this, "Numbers coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }
}
