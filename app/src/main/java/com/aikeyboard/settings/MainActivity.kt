package com.aikeyboard.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AIVoiceKeyboardTheme {
                SetupScreen(
                    requestAudioPermission = {
                        requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
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
    
    // Check status periodically
    LaunchedEffect(Unit) {
        while (true) {
            keyboardEnabled = isKeyboardEnabled(context)
            keyboardDefault = isKeyboardDefault(context)
            audioPermissionGranted = checkAudioPermission(context)
            delay(500)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
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
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Step 1: Enable Keyboard
        SetupStep(
            number = 1,
            title = "Enable Keyboard",
            description = if (keyboardEnabled) "Keyboard is enabled ✓" else "Allow AI Voice Keyboard in settings",
            isComplete = keyboardEnabled,
            onClick = { openInputMethodSettings(context) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Step 2: Set as Default
        SetupStep(
            number = 2,
            title = "Set as Default",
            description = if (keyboardDefault) "Keyboard is default ✓" else "Select as your default keyboard",
            isComplete = keyboardDefault,
            enabled = keyboardEnabled,
            onClick = { openDefaultKeyboardSettings(context) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Step 3: Microphone Permission
        SetupStep(
            number = 3,
            title = "Microphone Permission",
            description = if (audioPermissionGranted) "Microphone access granted ✓" else "Required for voice typing",
            isComplete = audioPermissionGranted,
            enabled = keyboardEnabled,
            onClick = { if (!audioPermissionGranted) requestAudioPermission() }
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Status Card
        if (keyboardEnabled && keyboardDefault && audioPermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "All Set!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "Open any app and start typing", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    }
                }
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Complete all steps above to start using the keyboard",
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Quick Settings Button
        OutlinedButton(
            onClick = { openInputMethodSettings(context) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Settings, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Keyboard Settings")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
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
    Card(
        onClick = if (enabled && !isComplete) onClick else {},
        colors = CardDefaults.cardColors(
            containerColor = when {
                isComplete -> Color(0xFF1B5E20)
                enabled -> Color(0xFF2D2D2D)
                else -> Color(0xFF1A1A1A)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isComplete) Color.Transparent else Color(0xFF4285F4),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                } else {
                    Text(text = number.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

// Helper Functions
fun isKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    val enabledInputMethods = imm.enabledInputMethodList
    return enabledInputMethods.any { it.packageName == context.packageName }
}

fun isKeyboardDefault(context: Context): Boolean {
    val defaultIme = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
    return defaultIme.contains(context.packageName)
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
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}

fun openDefaultKeyboardSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_SHOW_INPUT_METHOD_PICKER)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            openInputMethodSettings(context)
        }
    } catch (e: Exception) {
        openInputMethodSettings(context)
    }
}
