package com.aikeyboard.keyboard

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.aikeyboard.translation.ZAiClient
import com.aikeyboard.voice.GeminiVoiceClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun KeyboardContent(
    onTextCommit: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit
) {
    var currentPanel by remember { mutableStateOf("keyboard") }
    var currentLanguage by remember { mutableStateOf("en") }
    var isRecording by remember { mutableStateOf(false) }
    var recognizedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Voice recording state
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF1A1A2E))
    ) {
        // Top bar with panel buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF16213E))
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
                        isRecording = isRecording,
                        currentLanguage = currentLanguage,
                        recognizedText = recognizedText,
                        onLanguageChange = { currentLanguage = it },
                        onToggleRecording = {
                            if (isRecording) {
                                // Stop recording
                                stopRecording(mediaRecorder, audioFile) { file ->
                                    if (file != null) {
                                        scope.launch {
                                            val result = GeminiVoiceClient.instance.transcribeAudio(file, currentLanguage)
                                            result.onSuccess { text ->
                                                recognizedText = text
                                            }
                                            result.onFailure { e ->
                                                recognizedText = "Error: ${e.message}"
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Start recording
                                val (recorder, file) = startRecording(context)
                                mediaRecorder = recorder
                                audioFile = file
                            }
                            isRecording = !isRecording
                        },
                        onInsertText = { text ->
                            onTextCommit(text)
                            recognizedText = ""
                        }
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
                .background(Color(0xFF16213E))
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { currentLanguage = "en" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "en") Color(0xFF4285F4) else Color.Gray
                )
            ) { Text("EN", fontSize = 14.sp, fontWeight = FontWeight.Bold) }
            TextButton(
                onClick = { currentLanguage = "bn" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "bn") Color(0xFF4285F4) else Color.Gray
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
            contentColor = if (selected) Color(0xFF4285F4) else Color.White
        ),
        modifier = Modifier
            .background(
                if (selected) Color(0xFF4285F4).copy(alpha = 0.2f) else Color.Transparent,
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
        color = Color(0xFF2D2D2D)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(key, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun VoicePanel(
    isRecording: Boolean,
    currentLanguage: String,
    recognizedText: String,
    onLanguageChange: (String) -> Unit,
    onToggleRecording: () -> Unit,
    onInsertText: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilterChip(
                selected = currentLanguage == "en",
                onClick = { onLanguageChange("en") },
                label = { Text("English", fontSize = 12.sp) }
            )
            FilterChip(
                selected = currentLanguage == "bn",
                onClick = { onLanguageChange("bn") },
                label = { Text("বাংলা", fontSize = 12.sp) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FloatingActionButton(
            onClick = onToggleRecording,
            containerColor = if (isRecording) Color(0xFFEA4335) else Color(0xFF4285F4),
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                if (isRecording) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isRecording) "Listening..." else "Tap to speak",
            color = Color.Gray,
            fontSize = 12.sp
        )
        
        if (recognizedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(recognizedText, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { onInsertText(recognizedText) }) {
                        Text("Insert Text", color = Color(0xFF4285F4), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TranslatePanel(
    onInsertText: (String) -> Unit,
    currentLanguage: String,
    onLanguageChange: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    var isTranslating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Target language is opposite of current
    val targetLang = if (currentLanguage == "en") "bn" else "en"
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Language direction indicator
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
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            FilterChip(
                selected = currentLanguage == "bn",
                onClick = { onLanguageChange("bn") },
                label = { Text("বাং", fontSize = 10.sp) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Enter text to translate", color = Color.Gray, fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4285F4),
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
                    scope.launch {
                        val result = ZAiClient.translate(inputText, currentLanguage, targetLang)
                        result.onSuccess { text ->
                            translatedText = text
                        }
                        result.onFailure { e ->
                            translatedText = "Error: ${e.message}"
                        }
                        isTranslating = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
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
        
        if (translatedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
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
                        Text("Insert", color = Color(0xFF4285F4), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

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

// Voice Recording Helper Functions
fun startRecording(context: android.content.Context): Pair<MediaRecorder?, File?> {
    return try {
        val fileName = "${context.cacheDir.absolutePath}/voice_${System.currentTimeMillis()}.3gp"
        val file = File(fileName)
        
        val recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
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
        
        Pair(recorder, file)
    } catch (e: Exception) {
        Pair(null, null)
    }
}

fun stopRecording(
    recorder: MediaRecorder?,
    audioFile: File?,
    onComplete: (File?) -> Unit
) {
    try {
        recorder?.apply {
            stop()
            release()
        }
        onComplete(audioFile)
    } catch (e: Exception) {
        onComplete(null)
    }
}
