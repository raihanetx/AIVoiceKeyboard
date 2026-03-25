package com.aikeyboard.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "MainActivity"

// Colors - centralized
private val AppBackground = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF2D2D2D)
private val CardDisabled = Color(0xFF1A1A1A)
private val PrimaryBlue = Color(0xFF4285F4)
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
    
    // Check status periodically with isActive check
    LaunchedEffect(Unit) {
        // Initial check
        keyboardEnabled = isKeyboardEnabled(context)
        keyboardDefault = isKeyboardDefault(context)
        audioPermissionGranted = checkAudioPermission(context)
        
        // Continuous monitoring - with isActive check for proper cancellation
        while (isActive) {
            delay(500)
            keyboardEnabled = isKeyboardEnabled(context)
            keyboardDefault = isKeyboardDefault(context)
            audioPermissionGranted = checkAudioPermission(context)
        }
    }
    
    // Auto-request permission on first load if not granted
    LaunchedEffect(Unit) {
        if (!checkAudioPermission(context)) {
            delay(500) // Small delay to let UI settle
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
        Spacer(modifier = Modifier.height(40.dp))
        
        // App Icon/Logo
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(PrimaryBlue, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Keyboard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "AI Voice Keyboard",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Setup Guide",
            fontSize = 16.sp,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Step 1: Enable Keyboard
        SetupStep(
            number = 1,
            title = "Enable Keyboard",
            description = if (keyboardEnabled) "Keyboard is enabled ✓" else "Tap to enable keyboard in settings",
            isComplete = keyboardEnabled,
            onClick = { openInputMethodSettings(context) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Step 2: Set as Default
        SetupStep(
            number = 2,
            title = "Select Keyboard",
            description = if (keyboardDefault) "Keyboard is selected ✓" else "Tap to select as active keyboard",
            isComplete = keyboardDefault,
            enabled = keyboardEnabled,
            onClick = { showKeyboardPicker(context) }
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Step 3: Microphone Permission
        SetupStep(
            number = 3,
            title = "Microphone Permission",
            description = if (audioPermissionGranted) "Microphone access granted ✓" else "Tap to grant microphone permission",
            isComplete = audioPermissionGranted,
            enabled = keyboardEnabled,
            onClick = { 
                if (!audioPermissionGranted) {
                    requestAudioPermission()
                }
            }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Status Card
        if (keyboardEnabled && keyboardDefault && audioPermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "All Set!", 
                            color = Color.White, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open any app and tap a text field to use the keyboard", 
                            color = Color.White.copy(alpha = 0.8f), 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        } else {
            // Progress indicator
            val completedSteps = listOf(keyboardEnabled, keyboardDefault, audioPermissionGranted).count { it }
            Card(
                colors = CardDefaults.cardColors(containerColor = ProgressBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Setup Progress: $completedSteps/3",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completedSteps / 3f },
                        color = PrimaryBlue,
                        trackColor = CardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(horizontal = 16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Quick Settings Button
        Button(
            onClick = { openInputMethodSettings(context) },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Keyboard Settings", fontSize = 16.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Show keyboard picker button
        if (keyboardEnabled && !keyboardDefault) {
            OutlinedButton(
                onClick = { showKeyboardPicker(context) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Keyboard, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch to AI Keyboard", fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SetupStep(
    number: Int,
    title: String,
    description: String,
    isComplete: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (enabled && !isComplete) {
                Modifier.background(
                    if (isComplete) SuccessGreen else CardBackground,
                    RoundedCornerShape(12.dp)
                )
            } else {
                Modifier.background(
                    if (isComplete) SuccessGreen else if (enabled) CardBackground else CardDisabled,
                    RoundedCornerShape(12.dp)
                )
            }
        )
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isComplete -> SuccessGreen
                enabled -> CardBackground
                else -> CardDisabled
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = cardModifier,
        onClick = { if (enabled && !isComplete) onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isComplete) Color.Transparent else PrimaryBlue,
                        RoundedCornerShape(22.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = null, 
                        tint = Color.White, 
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = number.toString(), 
                        color = Color.White, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = if (enabled) Color.Gray else Color.DarkGray,
                    fontSize = 13.sp
                )
            }
            
            if (!isComplete && enabled) {
                Icon(
                    Icons.Default.ArrowForward, 
                    contentDescription = null, 
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==================== Helper Functions ====================

fun isKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledInputMethods = imm.enabledInputMethodList
        enabledInputMethods.any { it.packageName == context.packageName }
    } catch (e: Exception) {
        Log.e(TAG, "Error checking if keyboard is enabled", e)
        false
    }
}

fun isKeyboardDefault(context: Context): Boolean {
    return try {
        val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        defaultIme.contains(context.packageName)
    } catch (e: Exception) {
        Log.e(TAG, "Error checking if keyboard is default", e)
        false
    }
}

fun checkAudioPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
}

fun openInputMethodSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "Error opening input method settings", e)
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e2: Exception) {
            Log.e(TAG, "Error opening settings", e2)
        }
    }
}

fun showKeyboardPicker(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= 30) {
            val intent = Intent("android.settings.SHOW_INPUT_METHOD_PICKER")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error showing keyboard picker", e)
        openInputMethodSettings(context)
    }
}
