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
import com.aikeyboard.keyboard.AiKeyboardService
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val TAG = "MainActivity"

private val AppBackground = Color(0xFF1A1A2E)
private val CardBackground = Color(0xFF2D2D2D)
private val PrimaryBlue = Color(0xFF4285F4)
private val SuccessGreen = Color(0xFF1B5E20)
private val WarningOrange = Color(0xFFFF9800)

class MainActivity : ComponentActivity() {
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permissions: $permissions")
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AIVoiceKeyboardTheme {
                MainScreen(
                    requestMicPermission = {
                        permissionLauncher.launch(arrayOf(android.Manifest.permission.RECORD_AUDIO))
                    }
                )
            }
        }
    }
}

@Composable
fun MainScreen(requestMicPermission: () -> Unit) {
    val context = LocalContext.current
    
    // State
    var keyboardEnabled by remember { mutableStateOf(false) }
    var keyboardSelected by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(false) }
    
    // Check all statuses
    LaunchedEffect(Unit) {
        while (isActive) {
            keyboardEnabled = checkKeyboardEnabled(context)
            keyboardSelected = checkKeyboardSelected(context)
            micGranted = checkMicPermission(context)
            delay(300)
        }
    }
    
    // Auto request mic permission
    LaunchedEffect(keyboardSelected) {
        if (keyboardSelected && !micGranted) {
            delay(300)
            requestMicPermission()
        }
    }
    
    val allDone = keyboardEnabled && keyboardSelected && micGranted
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        
        // Header
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(if (allDone) SuccessGreen else PrimaryBlue, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (allDone) Icons.Default.Check else Icons.Default.Keyboard,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = if (allDone) "Ready to Use!" else "AI Voice Keyboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // ALL DONE SCREEN
        if (allDone) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Setup Complete!",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Open any app with a text field\ntap on it to use the keyboard",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Test button
            OutlinedButton(
                onClick = {
                    Toast.makeText(context, "Open WhatsApp, Messages, or any app with text field!", Toast.LENGTH_LONG).show()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("How to Use", fontSize = 15.sp)
            }
        } 
        // SETUP SCREEN
        else {
            // Step 1: Enable
            StatusCard(
                title = "Step 1: Enable Keyboard",
                isDone = keyboardEnabled,
                onClick = { openKeyboardSettings(context) }
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Step 2: Select
            StatusCard(
                title = "Step 2: Select Keyboard",
                subtitle = if (keyboardEnabled && !keyboardSelected) "Tap notification bar → Change keyboard" else null,
                isDone = keyboardSelected,
                enabled = keyboardEnabled,
                onClick = { switchKeyboard(context) }
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Step 3: Microphone
            StatusCard(
                title = "Step 3: Microphone Permission",
                isDone = micGranted,
                enabled = keyboardSelected,
                onClick = { requestMicPermission() }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Progress
            val done = listOf(keyboardEnabled, keyboardSelected, micGranted).count { it }
            LinearProgressIndicator(
                progress = { done / 3f },
                color = PrimaryBlue,
                trackColor = CardBackground,
                modifier = Modifier.fillMaxWidth().height(8.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$done of 3 complete",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Action buttons
            if (!keyboardEnabled) {
                Button(
                    onClick = { openKeyboardSettings(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Settings", fontSize = 15.sp)
                }
            } else if (!keyboardSelected) {
                Button(
                    onClick = { switchKeyboard(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Switch Keyboard", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = WarningOrange)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Swipe down notification bar and tap 'Change keyboard'",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatusCard(
    title: String,
    subtitle: String? = null,
    isDone: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isDone -> SuccessGreen
                enabled -> CardBackground
                else -> Color(0xFF1A1A1A)
            }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = { if (enabled && !isDone) onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        if (isDone) Color.Transparent else if (enabled) PrimaryBlue else Color.Gray,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = if (enabled) "?" else "✕",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = WarningOrange,
                        fontSize = 11.sp
                    )
                }
            }
            
            if (!isDone && enabled) {
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

// ==================== Helpers ====================

fun checkKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.enabledInputMethodList.any { it.packageName == context.packageName }
    } catch (e: Exception) {
        Log.e(TAG, "checkKeyboardEnabled error", e)
        false
    }
}

fun checkKeyboardSelected(context: Context): Boolean {
    return try {
        val defaultIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""
        defaultIme.contains(context.packageName)
    } catch (e: Exception) {
        Log.e(TAG, "checkKeyboardSelected error", e)
        false
    }
}

fun checkMicPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

fun openKeyboardSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "openKeyboardSettings error", e)
    }
}

fun switchKeyboard(context: Context) {
    try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
        Toast.makeText(context, "Select 'AI Voice Keyboard' from list", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Log.e(TAG, "switchKeyboard error", e)
        openKeyboardSettings(context)
    }
}
