package com.aikeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    private lateinit var tvStatus: TextView
    private lateinit var setupButtons: LinearLayout
    private lateinit var testArea: LinearLayout
    private lateinit var testInput: EditText
    private lateinit var btnEnableKeyboard: Button
    private lateinit var btnSelectKeyboard: Button
    private lateinit var btnClear: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        tvStatus = findViewById(R.id.tvStatus)
        setupButtons = findViewById(R.id.setupButtons)
        testArea = findViewById(R.id.testArea)
        testInput = findViewById(R.id.testInput)
        btnEnableKeyboard = findViewById(R.id.btnEnableKeyboard)
        btnSelectKeyboard = findViewById(R.id.btnSelectKeyboard)
        btnClear = findViewById(R.id.btnClear)
        
        btnEnableKeyboard.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }
        
        btnSelectKeyboard.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
        
        btnClear.setOnClickListener {
            testInput.text.clear()
        }
        
        // Focus on test input when test area is visible
        testInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Show keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(testInput, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun updateStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val isEnabled = imm.enabledInputMethodList.any { 
            it.packageName == packageName 
        }
        
        val isSelected = imm.currentInputMethodSubtype?.let {
            true // Keyboard is currently selected
        } ?: run {
            // Check if it's the default
            imm.enabledInputMethodList.any { it.id.contains(packageName) }
        }
        
        if (isEnabled) {
            tvStatus.text = "✓ Keyboard is enabled"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            setupButtons.visibility = android.view.View.GONE
            testArea.visibility = android.view.View.VISIBLE
            
            // Auto-focus on test input to show keyboard
            testInput.postDelayed({
                testInput.requestFocus()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(testInput, InputMethodManager.SHOW_IMPLICIT)
            }, 300)
        } else {
            tvStatus.text = "Keyboard not yet enabled. Follow steps below:"
            tvStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
            setupButtons.visibility = android.view.View.VISIBLE
            testArea.visibility = android.view.View.GONE
        }
    }
}
