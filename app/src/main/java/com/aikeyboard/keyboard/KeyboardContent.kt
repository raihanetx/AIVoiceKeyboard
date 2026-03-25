package com.aikeyboard.keyboard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.aikeyboard.AiKeyboardApp
import com.aikeyboard.translation.ZAiClient
import com.aikeyboard.voice.AndroidSpeechRecognizer
import com.aikeyboard.voice.GroqWhisperClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "KeyboardContent"

// Colors
private val KeyboardBackground = Color(0xFF1A1A2E)
private val KeyboardSurface = Color(0xFF16213E)
private val PrimaryBlue = Color(0xFF4285F4)
private val PrimaryRed = Color(0xFFEA4335)
private val PrimaryGreen = Color(0xFF34A853)
private val SuccessGreen = Color(0xFF1B5E20)
private val KeyBackground = Color(0xFF2D2D2D)
private val WarningOrange = Color(0xFFFF9800)

@Composable
fun KeyboardContent(
    onTextCommit: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    var currentPanel by remember { mutableStateOf("keyboard") }
    var currentLanguage by remember { mutableStateOf(AiKeyboardApp.defaultLanguage) }
    var sttEngine by remember { mutableStateOf(AiKeyboardApp.defaultSTTEngine) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(KeyboardBackground)
    ) {
        // Top bar with panel buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KeyboardSurface)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            PanelButton("ABC", currentPanel == "keyboard") { currentPanel = "keyboard" }
            PanelButton("🎤", currentPanel == "voice") { currentPanel = "voice" }
            PanelButton("🌐", currentPanel == "translate") { currentPanel = "translate" }
            PanelButton("😀", currentPanel == "emoji") { currentPanel = "emoji" }
        }

        HorizontalDivider(color = Color(0xFF333333))

        // Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            when (currentPanel) {
                "keyboard" -> {
                    if (currentLanguage == "en") {
                        EnglishKeyboardContent(
                            onKeyClick = { key ->
                                when (key) {
                                    "⌫" -> onDelete()
                                    "↵" -> onEnter()
                                    "Space" -> onTextCommit(" ")
                                    else -> onTextCommit(key)
                                }
                            }
                        )
                    } else {
                        BanglaKeyboardContent(
                            onKeyClick = { key -> onTextCommit(key) }
                        )
                    }
                }
                "voice" -> {
                    VoicePanel(
                        currentLanguage = currentLanguage,
                        sttEngine = sttEngine,
                        onLanguageChange = { currentLanguage = it },
                        onEngineChange = { sttEngine = it },
                        onTextCommit = onTextCommit
                    )
                }
                "translate" -> {
                    TranslatePanel(
                        onInsertText = onTextCommit,
                        currentLanguage = currentLanguage,
                        onLanguageChange = { currentLanguage = it }
                    )
                }
                "emoji" -> {
                    EmojiPanel(
                        onEmojiClick = { emoji -> onTextCommit(emoji) }
                    )
                }
            }
        }

        // Language switch
        HorizontalDivider(color = Color(0xFF333333))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KeyboardSurface)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { currentLanguage = "en" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "en") PrimaryBlue else Color.Gray
                )
            ) { Text("EN", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = { currentLanguage = "bn" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "bn") PrimaryBlue else Color.Gray
                )
            ) { Text("বাং", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun PanelButton(text: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (selected) PrimaryBlue else Color.White
        ),
        modifier = Modifier
            .background(
                if (selected) PrimaryBlue.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
    ) {
        Text(text, fontSize = 16.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun EnglishKeyboardContent(onKeyClick: (String) -> Unit) {
    val rows = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","😊","Space",".","↵")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    KeyButton(key = key, onClick = { onKeyClick(key) })
                }
            }
        }
    }
}

@Composable
fun BanglaKeyboardContent(onKeyClick: (String) -> Unit) {
    val rows = listOf(
        listOf("১","২","৩","৪","৫","৬","৭","৮","৯","০"),
        listOf("ৌ","ৈ","া","ী","ূ","ব","হ","গ","দ","জ"),
        listOf("ো","ে","্","ি","ু","প","র","ক","ত","চ"),
        listOf("ং","ম","ন","ণ","স","ও","য","শ","খ","ঃ")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key -> KeyButton(key = key, onClick = { onKeyClick(key) }) }
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(key: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .padding(2.dp)
            .height(38.dp)
            .weight(1f),
        shape = RoundedCornerShape(4.dp),
        color = KeyBackground
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(key, color = Color.White, fontSize = 16.sp)
        }
    }
}

// ==================== VOICE PANEL ====================

@Composable
fun VoicePanel(
    currentLanguage: String,
    sttEngine: String,
    onLanguageChange: (String) -> Unit,
    onEngineChange: (String) -> Unit,
    onTextCommit: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var isListening by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var partialText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // MediaRecorder for Groq (file-based)
    var mediaRecorderState by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFileState by remember { mutableStateOf<File?>(null) }

    // Android SpeechRecognizer
    var androidRecognizer by remember { mutableStateOf<AndroidSpeechRecognizer?>(null) }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorderState?.let { recorder ->
                try { recorder.stop() } catch (e: Exception) { Log.e(TAG, "Error stopping", e) }
                try { recorder.release() } catch (e: Exception) { Log.e(TAG, "Error releasing", e) }
            }
            androidRecognizer?.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Engine Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = sttEngine == "groq",
                onClick = { onEngineChange("groq") },
                label = { Text("Groq (Online)", fontSize = 11.sp) },
                leadingIcon = {
                    if (sttEngine == "groq") Icon(Icons.Default.Cloud, null, modifier = Modifier.size(14.dp))
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = sttEngine == "android",
                onClick = { onEngineChange("android") },
                label = { Text("Android (Offline)", fontSize = 11.sp) },
                leadingIcon = {
                    if (sttEngine == "android") Icon(Icons.Default.PhoneAndroid, null, modifier = Modifier.size(14.dp))
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Language Selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = currentLanguage == "en",
                onClick = { onLanguageChange("en") },
                label = { Text("English", fontSize = 11.sp) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = currentLanguage == "bn",
                onClick = { onLanguageChange("bn") },
                label = { Text("বাংলা", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mic Button
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    // STOP LISTENING
                    if (sttEngine == "android") {
                        androidRecognizer?.stopListening()
                        androidRecognizer = null
                    } else {
                        // Stop Groq recording
                        val recorder = mediaRecorderState
                        val audioFile = audioFileState
                        stopRecording(recorder, audioFile) { file ->
                            if (file != null && file.exists() && file.length() > 0) {
                                scope.launch {
                                    statusMessage = "Transcribing..."
                                    val result = GroqWhisperClient.instance.transcribeAudio(file, currentLanguage)
                                    result.onSuccess { text ->
                                        recognizedText = text
                                        errorMessage = null
                                        statusMessage = ""
                                    }
                                    result.onFailure { e ->
                                        errorMessage = e.message
                                        statusMessage = ""
                                        Log.e(TAG, "Groq error", e)
                                    }
                                    try { file.delete() } catch (e: Exception) { }
                                }
                            } else {
                                errorMessage = "Recording failed"
                                statusMessage = ""
                            }
                        }
                        mediaRecorderState = null
                        audioFileState = null
                    }
                    isListening = false
                } else {
                    // START LISTENING
                    errorMessage = null
                    recognizedText = ""
                    partialText = ""

                    // Check permission
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                        errorMessage = "Microphone permission required"
                        return@FloatingActionButton
                    }

                    if (sttEngine == "android") {
                        // Android SpeechRecognizer
                        if (!AndroidSpeechRecognizer.isAvailable(context)) {
                            errorMessage = "Speech recognition not available"
                            return@FloatingActionButton
                        }

                        val recognizer = AndroidSpeechRecognizer(context)
                        androidRecognizer = recognizer
                        isListening = true

                        scope.launch {
                            recognizer.startListening(currentLanguage).collect { state ->
                                when (state) {
                                    is AndroidSpeechRecognizer.RecognitionState.Ready -> {
                                        statusMessage = state.message
                                    }
                                    is AndroidSpeechRecognizer.RecognitionState.PartialResult -> {
                                        partialText = state.text
                                    }
                                    is AndroidSpeechRecognizer.RecognitionState.FinalResult -> {
                                        recognizedText = state.text
                                        partialText = ""
                                        statusMessage = ""
                                        isListening = false
                                    }
                                    is AndroidSpeechRecognizer.RecognitionState.Error -> {
                                        errorMessage = state.message
                                        statusMessage = ""
                                        isListening = false
                                    }
                                    is AndroidSpeechRecognizer.RecognitionState.Silent -> {}
                                }
                            }
                        }
                    } else {
                        // Groq Whisper - record to file
                        if (AiKeyboardApp.GROQ_API_KEY.isBlank()) {
                            errorMessage = "Groq API key not set. Get free key from console.groq.com"
                            return@FloatingActionButton
                        }

                        val (recorder, file) = startRecording(context)
                        if (recorder != null && file != null) {
                            mediaRecorderState = recorder
                            audioFileState = file
                            isListening = true
                            statusMessage = "Recording... Tap to stop"
                        } else {
                            errorMessage = "Failed to start recording"
                        }
                    }
                }
            },
            containerColor = when {
                isListening -> PrimaryRed
                sttEngine == "groq" -> PrimaryBlue
                else -> PrimaryGreen
            },
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Status
        Text(
            text = statusMessage.ifEmpty { if (isListening) "Listening..." else "Tap to speak" },
            color = Color.Gray,
            fontSize = 11.sp
        )

        // Error message
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage!!,
                color = PrimaryRed,
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }

        // Partial result (Android only)
        if (partialText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = KeyBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Listening...", color = Color.Gray, fontSize = 10.sp)
                    Text(partialText, color = Color.White, fontSize = 12.sp)
                }
            }
        }

        // Final result
        if (recognizedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SuccessGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(recognizedText, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onTextCommit(recognizedText)
                            recognizedText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Text("Insert Text", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==================== TRANSLATE PANEL ====================

@Composable
fun TranslatePanel(
    onInsertText: (String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var translateError by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val targetLang = if (currentLanguage == "en") "bn" else "en"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Language direction
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = currentLanguage == "en",
                onClick = { onLanguageChange("en") },
                label = { Text("EN", fontSize = 10.sp) }
            )
            Icon(Icons.Default.ArrowForward, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            FilterChip(
                selected = currentLanguage == "bn",
                onClick = { onLanguageChange("bn") },
                label = { Text("বাং", fontSize = 10.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                translateError = null
            },
            placeholder = { Text("Enter text to translate", color = Color.Gray, fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    isTranslating = true
                    translateError = null
                    scope.launch {
                        val result = ZAiClient.translate(inputText, currentLanguage, targetLang)
                        result.onSuccess { text ->
                            translatedText = text
                        }
                        result.onFailure { e ->
                            translateError = e.message ?: "Translation failed"
                            Log.e(TAG, "Translation failed", e)
                        }
                        isTranslating = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier.fillMaxWidth().height(36.dp),
            enabled = !isTranslating && inputText.isNotBlank()
        ) {
            if (isTranslating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Translate", fontSize = 12.sp)
            }
        }

        if (!translateError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = translateError!!,
                color = PrimaryRed,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (translatedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = KeyBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        translatedText,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = { onInsertText(translatedText) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Insert", color = PrimaryBlue, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ==================== EMOJI PANEL ====================

@Composable
fun EmojiPanel(onEmojiClick: (String) -> Unit) {
    val emojis = listOf(
        listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "😊"),
        listOf("❤️", "🔥", "✨", "👍", "👎", "👏", "🎉", "💯", "🙌", "💪"),
        listOf("🥰", "😍", "🤩", "😘", "😎", "🤔", "😢", "😭", "😤", "🤗"),
        listOf("🙏", "👋", "🤝", "✌️", "🤞", "👌", "✋", "👏", "🤲", "👐")
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        emojis.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { onEmojiClick(emoji) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

// ==================== RECORDING HELPERS ====================

fun startRecording(context: Context): Pair<MediaRecorder?, File?> {
    return try {
        val cacheDir = context.cacheDir
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val fileName = "${cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.3gp"
        val file = File(fileName)

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        recorder.setOutputFile(fileName)
        recorder.prepare()
        recorder.start()

        Log.d(TAG, "Recording started: $fileName")
        Pair(recorder, file)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to start recording", e)
        Pair(null, null)
    }
}

fun stopRecording(
    recorder: MediaRecorder?,
    audioFile: File?,
    onComplete: (File?) -> Unit
) {
    Log.d(TAG, "Stopping recording")

    try {
        recorder?.stop()
        Log.d(TAG, "Recording stopped")
    } catch (e: Exception) {
        Log.e(TAG, "Error stopping recorder", e)
    } finally {
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing recorder", e)
        }
    }

    onComplete(audioFile)
}
