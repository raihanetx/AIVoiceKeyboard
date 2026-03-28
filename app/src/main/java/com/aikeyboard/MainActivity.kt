package com.aikeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }
        
        findViewById<Button>(R.id.btnSelectKeyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    }
    
    override fun onResume() {
        super.onResume()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        
        findViewById<TextView>(R.id.tvStatus).text = if (enabled) {
            "✅ Keyboard enabled!\n\nOpen any app with text input (Messages, WhatsApp, etc.)\nand tap on a text field to use the keyboard."
        } else {
            "1. Tap 'Enable Keyboard' below\n2. Enable 'AI Voice Keyboard'\n3. Come back and tap 'Select Keyboard'"
        }
    }
}
