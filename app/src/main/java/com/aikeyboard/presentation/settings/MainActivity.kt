package com.aikeyboard.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aikeyboard.R

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var step1Card: CardView
    private lateinit var step1Number: TextView
    private lateinit var step1Text: TextView
    private lateinit var step1Arrow: ImageView
    private lateinit var step1Check: ImageView
    
    private lateinit var step2Card: CardView
    private lateinit var step2Number: TextView
    private lateinit var step2Text: TextView
    private lateinit var step2Hint: TextView
    private lateinit var step2Arrow: ImageView
    private lateinit var step2Check: ImageView
    
    private lateinit var step3Card: CardView
    private lateinit var step3Number: TextView
    private lateinit var step3Text: TextView
    private lateinit var step3Arrow: ImageView
    private lateinit var step3Check: ImageView
    
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var progressText: TextView
    private lateinit var statusCardsContainer: View
    private lateinit var successCard: CardView
    private lateinit var mainActionButton: Button
    private lateinit var hintCard: CardView
    private lateinit var headerIcon: ImageView
    private lateinit var titleText: TextView

    private var keyboardEnabled = false
    private var keyboardSelected = false
    private var micGranted = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permission result: $permissions")
        checkAllStates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(TAG, "=== MainActivity onCreate ===")
        
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        step1Card = findViewById(R.id.step1Card)
        step1Number = findViewById(R.id.step1Number)
        step1Text = findViewById(R.id.step1Text)
        step1Arrow = findViewById(R.id.step1Arrow)
        step1Check = findViewById(R.id.step1Check)
        
        step2Card = findViewById(R.id.step2Card)
        step2Number = findViewById(R.id.step2Number)
        step2Text = findViewById(R.id.step2Text)
        step2Hint = findViewById(R.id.step2Hint)
        step2Arrow = findViewById(R.id.step2Arrow)
        step2Check = findViewById(R.id.step2Check)
        
        step3Card = findViewById(R.id.step3Card)
        step3Number = findViewById(R.id.step3Number)
        step3Text = findViewById(R.id.step3Text)
        step3Arrow = findViewById(R.id.step3Arrow)
        step3Check = findViewById(R.id.step3Check)
        
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        statusCardsContainer = findViewById(R.id.statusCardsContainer)
        successCard = findViewById(R.id.successCard)
        mainActionButton = findViewById(R.id.mainActionButton)
        hintCard = findViewById(R.id.hintCard)
        headerIcon = findViewById(R.id.headerIcon)
        titleText = findViewById(R.id.titleText)
    }

    private fun setupClickListeners() {
        step1Card.setOnClickListener { openKeyboardSettings() }
        step2Card.setOnClickListener { switchKeyboard() }
        step3Card.setOnClickListener { requestMicPermission() }
        mainActionButton.setOnClickListener { onMainActionClick() }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== MainActivity onResume ===")
        checkAllStates()
    }

    private fun checkAllStates() {
        keyboardEnabled = checkKeyboardEnabled()
        keyboardSelected = checkKeyboardSelected()
        micGranted = checkMicPermission()
        
        Log.d(TAG, "States: enabled=$keyboardEnabled, selected=$keyboardSelected, mic=$micGranted")
        
        updateUI()
    }

    private fun updateUI() {
        val done = listOf(keyboardEnabled, keyboardSelected, micGranted).count { it }
        
        // Update progress
        progressBar.progress = done
        progressText.text = "$done of 3 complete"
        
        // Step 1
        updateStepCard(
            card = step1Card,
            number = step1Number,
            text = step1Text,
            arrow = step1Arrow,
            check = step1Check,
            isDone = keyboardEnabled,
            isEnabled = true
        )
        
        // Step 2
        updateStepCard(
            card = step2Card,
            number = step2Number,
            text = step2Text,
            arrow = step2Arrow,
            check = step2Check,
            isDone = keyboardSelected,
            isEnabled = keyboardEnabled
        )
        step2Hint.isVisible = keyboardEnabled && !keyboardSelected
        step2Text.setTextColor(
            if (keyboardEnabled) ContextCompat.getColor(this, android.R.color.white)
            else ContextCompat.getColor(this, android.R.color.darker_gray)
        )
        
        // Step 3
        updateStepCard(
            card = step3Card,
            number = step3Number,
            text = step3Text,
            arrow = step3Arrow,
            check = step3Check,
            isDone = micGranted,
            isEnabled = keyboardSelected
        )
        step3Text.setTextColor(
            if (keyboardSelected) ContextCompat.getColor(this, android.R.color.white)
            else ContextCompat.getColor(this, android.R.color.darker_gray)
        )
        
        // Show/hide success card
        val allDone = keyboardEnabled && keyboardSelected && micGranted
        statusCardsContainer.isVisible = !allDone
        successCard.isVisible = allDone
        
        // Update header
        if (allDone) {
            titleText.text = "Ready to Use!"
            mainActionButton.text = "How to Use"
            mainActionButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_color)
        } else {
            titleText.text = "AI Voice Keyboard"
            mainActionButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
            updateMainButton()
        }
        
        // Show hint for step 2
        hintCard.isVisible = keyboardEnabled && !keyboardSelected
    }

    private fun updateStepCard(
        card: CardView,
        number: TextView,
        text: TextView,
        arrow: ImageView,
        check: ImageView,
        isDone: Boolean,
        isEnabled: Boolean
    ) {
        when {
            isDone -> {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.success_color))
                number.setBackgroundResource(R.drawable.step_bg_success)
                number.text = "✓"
                number.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                arrow.isVisible = false
                check.isVisible = false
            }
            isEnabled -> {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg))
                number.setBackgroundResource(R.drawable.step_bg)
                number.text = (listOf(step1Card, step2Card, step3Card).indexOf(card) + 1).toString()
                number.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                arrow.isVisible = true
                check.isVisible = false
            }
            else -> {
                card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.card_bg_disabled))
                number.setBackgroundResource(R.drawable.step_bg_disabled)
                number.text = (listOf(step1Card, step2Card, step3Card).indexOf(card) + 1).toString()
                number.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                arrow.isVisible = false
                check.isVisible = false
            }
        }
    }

    private fun updateMainButton() {
        when {
            !keyboardEnabled -> {
                mainActionButton.text = "Open Settings"
                mainActionButton.isVisible = true
            }
            !keyboardSelected -> {
                mainActionButton.text = "Switch Keyboard"
                mainActionButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.success_color)
                mainActionButton.isVisible = true
            }
            !micGranted -> {
                mainActionButton.text = "Grant Microphone Permission"
                mainActionButton.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary_color)
                mainActionButton.isVisible = true
            }
            else -> {
                mainActionButton.isVisible = false
            }
        }
    }

    private fun onMainActionClick() {
        val allDone = keyboardEnabled && keyboardSelected && micGranted
        if (allDone) {
            Toast.makeText(this, "Open WhatsApp, Messages, or any app with text field!", Toast.LENGTH_LONG).show()
            return
        }
        
        when {
            !keyboardEnabled -> openKeyboardSettings()
            !keyboardSelected -> switchKeyboard()
            !micGranted -> requestMicPermission()
        }
    }

    private fun checkKeyboardEnabled(): Boolean {
        return try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            val enabledList = imm.enabledInputMethodList
            val packageName = packageName
            
            Log.d(TAG, "Checking keyboard enabled...")
            Log.d(TAG, "  Package: $packageName")
            Log.d(TAG, "  Enabled IMEs: ${enabledList.size}")
            
            enabledList.forEach { info ->
                Log.d(TAG, "  - ${info.packageName}/${info.id}")
            }
            
            val isEnabled = enabledList.any { it.packageName == packageName }
            Log.d(TAG, "  Result: $isEnabled")
            
            isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "checkKeyboardEnabled error", e)
            false
        }
    }

    private fun checkKeyboardSelected(): Boolean {
        return try {
            val defaultIme = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.DEFAULT_INPUT_METHOD
            ) ?: ""
            
            val packageName = packageName
            val serviceClassName = "com.aikeyboard.presentation.keyboard.AiKeyboardService"
            val expectedId = "$packageName/$serviceClassName"
            
            Log.d(TAG, "Checking keyboard selected...")
            Log.d(TAG, "  Package: $packageName")
            Log.d(TAG, "  Current default IME: $defaultIme")
            Log.d(TAG, "  Expected ID: $expectedId")
            
            val isSelected = defaultIme.contains(packageName) ||
                    defaultIme == expectedId ||
                    defaultIme.endsWith("/$serviceClassName")
            
            Log.d(TAG, "  Result: $isSelected")
            
            isSelected
        } catch (e: Exception) {
            Log.e(TAG, "checkKeyboardSelected error", e)
            false
        }
    }

    private fun checkMicPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG, "checkMicPermission: $result")
        return result
    }

    private fun openKeyboardSettings() {
        try {
            Log.d(TAG, "Opening keyboard settings...")
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "openKeyboardSettings error", e)
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun switchKeyboard() {
        try {
            Log.d(TAG, "Showing input method picker...")
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            
            try {
                imm.showInputMethodPicker()
                Toast.makeText(this, "Select 'AI Voice Keyboard' from the list", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.w(TAG, "showInputMethodPicker failed, trying alternative", e)
                openKeyboardSettings()
            }
        } catch (e: Exception) {
            Log.e(TAG, "switchKeyboard error", e)
            openKeyboardSettings()
        }
    }

    private fun requestMicPermission() {
        Log.d(TAG, "Requesting RECORD_AUDIO permission")
        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
    }
}
