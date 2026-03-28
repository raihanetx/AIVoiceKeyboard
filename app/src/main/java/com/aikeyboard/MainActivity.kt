package com.aikeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        findViewById<Button>(R.id.btnEnableKeyboard).setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }
        
        findViewById<Button>(R.id.btnSelectKeyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        
        updateStatus()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun updateStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { 
            it.packageName == packageName 
        }
        
        val statusText = findViewById<TextView>(R.id.tvStatus)
        if (enabled) {
            statusText.text = "✓ Keyboard is enabled! Tap 'Select Keyboard' to use it."
            statusText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            statusText.text = "Keyboard not yet enabled. Tap 'Enable Keyboard' to proceed."
            statusText.setTextColor(getColor(android.R.color.holo_orange_dark))
        }
    }
}
