package com.aikeyboard.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "MainActivity"

// Colors
private val AppBackground = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF2D2D2D)
private val CardDisabled = Color(0xFF1A1A1A)
private val PrimaryBlue = Color(0xFF4285F4)
private val PrimaryRed = Color(0xFFEA4335)
private val SuccessGreen = Color(0xFF1B5E20)
private val ProgressBackground = Color(0xFF37474F)

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permission result: $permissions")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity created")
        
        setContent {
            AIVoiceKeyboardTheme {
                SetupScreen(
                    requestAudioPermission = {
                        requestPermissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                    }
                )
            }
        }
    }
}

@Composable
fun SetupScreen(requestAudioPermission: () -> Unit) {
    val context = LocalContext.current
    
    var keyboardEnabled by remember { mutableStateOf(false) }
    var keyboardDefault by remember { mutableStateOf(false) }
    var audioPermissionGranted by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(false) }
    
    // Check status periodically
    LaunchedEffect(Unit) {
        keyboardEnabled = isKeyboardEnabled(context)
        keyboardDefault = isKeyboardDefault(context)
        audioPermissionGranted = checkAudioPermission(context)
        
        while (isActive) {
            delay(500)
            keyboardEnabled = isKeyboardEnabled(context)
            keyboardDefault = isKeyboardDefault(context)
            audioPermissionGranted = checkAudioPermission(context)
        }
    }
    
    // Auto-request permission
    LaunchedEffect(Unit) {
        if (!checkAudioPermission(context)) {
            delay(500)
            requestAudioPermission()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        // Logo
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(PrimaryBlue, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "AI Voice Keyboard",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "Setup Guide",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Step 1: Enable Keyboard
        SetupStepCard(
            number = 1,
            title = "Enable Keyboard",
            description = if (keyboardEnabled) "Enabled ✓" else "Tap to enable in settings",
            isComplete = keyboardEnabled,
            onClick = { 
                showInstructions = false
                openKeyboardSettings(context) 
            }
        )
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Step 2: Select Keyboard - IMPORTANT STEP
        SetupStepCard(
            number = 2,
            title = "Select Keyboard",
            description = if (keyboardDefault) "Selected ✓" else "Tap to switch keyboard",
            isComplete = keyboardDefault,
            enabled = keyboardEnabled,
            onClick = { 
                showInstructions = true
                switchKeyboard(context) 
            }
        )
        
        // Show instructions when trying to select keyboard
        if (showInstructions && keyboardEnabled && !keyboardDefault) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0D47A1)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "How to Select Keyboard:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Notification panel swipe down করুন\n" +
                               "2. 'Change keyboard' বা keyboard icon ট্যাপ করুন\n" +
                               "3. 'AI Voice Keyboard' সিলেক্ট করুন\n" +
                               "4. অথবা Settings > Language & Input এ যান",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        // Step 3: Microphone Permission
        SetupStepCard(
            number = 3,
            title = "Microphone",
            description = if (audioPermissionGranted) "Granted ✓" else "Tap for microphone access",
            isComplete = audioPermissionGranted,
            enabled = keyboardEnabled,
            onClick = { 
                showInstructions = false
                if (!audioPermissionGranted) {
                    requestAudioPermission()
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status
        if (keyboardEnabled && keyboardDefault && audioPermissionGranted) {
            // Success
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ready to Use!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Open any app and tap a text field",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            // Progress
            val completed = listOf(keyboardEnabled, keyboardDefault, audioPermissionGranted).count { it }
            Card(
                colors = CardDefaults.cardColors(containerColor = ProgressBackground),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Progress: $completed / 3",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { completed / 3f },
                        color = PrimaryBlue,
                        trackColor = CardBackground,
                        modifier = Modifier.fillMaxWidth().height(6.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom buttons
        Button(
            onClick = { 
                showInstructions = false
                openKeyboardSettings(context) 
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Keyboard Settings", fontSize = 15.sp)
        }
        
        // Quick switch button - always show if enabled but not default
        if (keyboardEnabled && !keyboardDefault) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = { 
                    showInstructions = true
                    switchKeyboard(context) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34A853)),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch Keyboard Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SetupStepCard(
    number: Int,
    title: String,
    description: String,
    isComplete: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isComplete -> SuccessGreen
                enabled -> CardBackground
                else -> CardDisabled
            }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled && !isComplete) onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isComplete) Color.Transparent else PrimaryBlue,
                        RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = number.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
                Text(
                    text = description,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    fontSize = 12.sp
                )
            }
            
            if (!isComplete && enabled) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==================== Helper Functions ====================

fun isKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val found = enabledMethods.any { it.packageName == context.packageName }
        Log.d(TAG, "Keyboard enabled: $found, package: ${context.packageName}")
        found
    } catch (e: Exception) {
        Log.e(TAG, "Error checking keyboard enabled", e)
        false
    }
}

fun isKeyboardDefault(context: Context): Boolean {
    return try {
        val defaultIme = Settings.Secure.getString(
            context.contentResolver, 
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        val isDefault = defaultIme.contains(context.packageName)
        Log.d(TAG, "Default IME: $defaultIme, isDefault: $isDefault")
        isDefault
    } catch (e: Exception) {
        Log.e(TAG, "Error checking default keyboard", e)
        false
    }
}

fun checkAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, 
        android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

fun openKeyboardSettings(context: Context) {
    Log.d(TAG, "Opening keyboard settings")
    try {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to open input method settings", e)
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e2: Exception) {
            Log.e(TAG, "Failed to open settings", e2)
        }
    }
}

fun switchKeyboard(context: Context) {
    Log.d(TAG, "Attempting to switch keyboard")
    
    try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        
        // Method 1: Show input method picker directly
        imm.showInputMethodPicker()
        
        // Show toast to guide user
        Toast.makeText(
            context,
            "Select 'AI Voice Keyboard' from the list",
            Toast.LENGTH_LONG
        ).show()
        
        Log.d(TAG, "showInputMethodPicker() called")
        
    } catch (e: Exception) {
        Log.e(TAG, "showInputMethodPicker failed", e)
        
        // Method 2: Try intent for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent("android.settings.SHOW_INPUT_METHOD_PICKER")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "SHOW_INPUT_METHOD_PICKER intent launched")
            } catch (e2: Exception) {
                Log.e(TAG, "SHOW_INPUT_METHOD_PICKER intent failed", e2)
                // Fallback to settings
                openKeyboardSettings(context)
            }
        } else {
            // Fallback to settings
            openKeyboardSettings(context)
        }
    }
}
