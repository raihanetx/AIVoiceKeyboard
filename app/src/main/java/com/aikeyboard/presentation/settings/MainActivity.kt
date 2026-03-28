package com.aikeyboard.presentation.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.presentation.theme.AIVoiceKeyboardTheme
import com.aikeyboard.presentation.theme.AppColor

private const val TAG = "MainActivity"

/**
 * Main settings activity for the keyboard app
 * 
 * Handles keyboard setup and permission requests.
 */
class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d(TAG, "Permission result: $permissions")
        permissions.entries.forEach { (perm, granted) ->
            Log.d(TAG, "  $perm -> $granted")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== MainActivity onCreate ===")

        setContent {
            AIVoiceKeyboardTheme {
                MainScreen(
                    requestMicPermission = {
                        Log.d(TAG, "Requesting RECORD_AUDIO permission")
                        permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "=== MainActivity onResume ===")
    }
}

@Composable
fun MainScreen(requestMicPermission: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State
    var keyboardEnabled by remember { mutableStateOf(false) }
    var keyboardSelected by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(false) }
    var checkCount by remember { mutableStateOf(0) }

    // Check state on lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE,
                Lifecycle.Event.ON_START,
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "Lifecycle event: $event - checking states...")
                    keyboardEnabled = checkKeyboardEnabled(context)
                    keyboardSelected = checkKeyboardSelected(context)
                    micGranted = checkMicPermission(context)
                    checkCount++
                    Log.d(TAG, "State check #$checkCount: enabled=$keyboardEnabled, selected=$keyboardSelected, mic=$micGranted")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic check
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(AppConstants.STATE_CHECK_INTERVAL)
            keyboardEnabled = checkKeyboardEnabled(context)
            keyboardSelected = checkKeyboardSelected(context)
            micGranted = checkMicPermission(context)
        }
    }

    // Auto request mic when keyboard is selected
    LaunchedEffect(keyboardSelected, micGranted) {
        if (keyboardSelected && !micGranted) {
            Log.d(TAG, "Keyboard selected, auto-requesting mic permission")
            kotlinx.coroutines.delay(300)
            requestMicPermission()
        }
    }

    val allDone = keyboardEnabled && keyboardSelected && micGranted

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColor.Background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        // Header
        Box(
            modifier = Modifier
                .size(70.dp)
                .background(
                    if (allDone) AppColor.Success else AppColor.Primary,
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (allDone) Icons.Default.Check else Icons.Default.Keyboard,
                contentDescription = null,
                tint = AppColor.OnPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (allDone) "Ready to Use!" else "AI Voice Keyboard",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = AppColor.OnBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (allDone) {
            SetupCompleteCard()
        } else {
            SetupCards(
                keyboardEnabled = keyboardEnabled,
                keyboardSelected = keyboardSelected,
                micGranted = micGranted,
                onEnableClick = { openKeyboardSettings(context) },
                onSelectClick = { switchKeyboard(context) },
                onMicClick = { requestMicPermission() }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun SetupCompleteCard() {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = AppColor.Success),
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
                tint = AppColor.OnPrimary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Setup Complete!",
                color = AppColor.OnPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Open any app with a text field\ntap on it to use the keyboard",
                color = AppColor.OnPrimary.copy(alpha = 0.9f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    OutlinedButton(
        onClick = {
            Toast.makeText(
                context,
                "Open WhatsApp, Messages, or any app with text field!",
                Toast.LENGTH_LONG
            ).show()
        },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColor.OnBackground),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(Icons.Default.Info, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("How to Use", fontSize = 15.sp)
    }
}

@Composable
fun SetupCards(
    keyboardEnabled: Boolean,
    keyboardSelected: Boolean,
    micGranted: Boolean,
    onEnableClick: () -> Unit,
    onSelectClick: () -> Unit,
    onMicClick: () -> Unit
) {
    StatusCard(
        title = "Step 1: Enable Keyboard",
        isDone = keyboardEnabled,
        onClick = onEnableClick
    )

    Spacer(modifier = Modifier.height(10.dp))

    StatusCard(
        title = "Step 2: Select Keyboard",
        subtitle = if (keyboardEnabled && !keyboardSelected) "Tap notification bar → Change keyboard" else null,
        isDone = keyboardSelected,
        enabled = keyboardEnabled,
        onClick = onSelectClick
    )

    Spacer(modifier = Modifier.height(10.dp))

    StatusCard(
        title = "Step 3: Microphone Permission",
        isDone = micGranted,
        enabled = keyboardSelected,
        onClick = onMicClick
    )

    Spacer(modifier = Modifier.height(20.dp))

    val done = listOf(keyboardEnabled, keyboardSelected, micGranted).count { it }
    LinearProgressIndicator(
        progress = { done / 3f },
        color = AppColor.Primary,
        trackColor = AppColor.CardBackground,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "$done of 3 complete",
        color = AppColor.TextSecondary,
        fontSize = 14.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    // Main action button
    when {
        !keyboardEnabled -> {
            Button(
                onClick = onEnableClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Settings", fontSize = 15.sp)
            }
        }
        !keyboardSelected -> {
            Button(
                onClick = onSelectClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.Success),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Switch Keyboard", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = AppColor.Warning.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = AppColor.Warning)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Swipe down notification bar and tap 'Change keyboard'",
                        color = AppColor.OnBackground,
                        fontSize = 13.sp
                    )
                }
            }
        }
        !micGranted -> {
            Button(
                onClick = onMicClick,
                colors = ButtonDefaults.buttonColors(containerColor = AppColor.Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Grant Microphone Permission", fontSize = 15.sp)
            }
        }
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
                isDone -> AppColor.Success
                enabled -> AppColor.CardBackground
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
                        if (isDone) AppColor.Transparent
                        else if (enabled) AppColor.Primary
                        else AppColor.TextSecondary,
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = AppColor.OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = if (enabled) "?" else "✕",
                        color = AppColor.OnPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) AppColor.OnBackground else AppColor.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = AppColor.Warning,
                        fontSize = 11.sp
                    )
                }
            }

            if (!isDone && enabled) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = AppColor.OnBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==================== Helper Functions ====================

fun checkKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledList = imm.enabledInputMethodList
        val packageName = context.packageName

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

fun checkKeyboardSelected(context: Context): Boolean {
    return try {
        val defaultIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: ""

        val packageName = context.packageName
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

fun checkMicPermission(context: Context): Boolean {
    val result = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    Log.d(TAG, "checkMicPermission: $result")
    return result
}

fun openKeyboardSettings(context: Context) {
    try {
        Log.d(TAG, "Opening keyboard settings...")
        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Log.e(TAG, "openKeyboardSettings error", e)
        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
    }
}

fun switchKeyboard(context: Context) {
    try {
        Log.d(TAG, "Showing input method picker...")
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        try {
            imm.showInputMethodPicker()
            Toast.makeText(context, "Select 'AI Voice Keyboard' from the list", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.w(TAG, "showInputMethodPicker failed, trying alternative", e)
            openKeyboardSettings(context)
        }
    } catch (e: Exception) {
        Log.e(TAG, "switchKeyboard error", e)
        openKeyboardSettings(context)
    }
}
