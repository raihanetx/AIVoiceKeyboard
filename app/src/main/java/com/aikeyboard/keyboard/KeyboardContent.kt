package com.aikeyboard.keyboard

import android.content.pm.PackageManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import android.Manifest

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
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFF1A1A2E))
    ) {
        // Top bar
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
                        EnglishKeyboard(
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
                        BanglaKeyboard(
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
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                isRecording = !isRecording
                                // Voice recognition logic here
                            }
                        },
                        onInsertText = { text ->
                            onTextCommit(text)
                            recognizedText = ""
                        }
                    )
                }
                "translate" -> {
                    TranslatePanel(
                        onInsertText = onTextCommit
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
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(
                onClick = { currentLanguage = "en" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "en") Color(0xFF4285F4) else Color.Gray
                )
            ) { Text("EN", fontSize = 12.sp) }
            TextButton(
                onClick = { currentLanguage = "bn" },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (currentLanguage == "bn") Color(0xFF4285F4) else Color.Gray
                )
            ) { Text("বাং", fontSize = 12.sp) }
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
        Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
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
                label = { Text("EN", fontSize = 12.sp) }
            )
            FilterChip(
                selected = currentLanguage == "bn",
                onClick = { onLanguageChange("bn") },
                label = { Text("বাং", fontSize = 12.sp) }
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
                contentDescription = null
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
            TextButton(onClick = { onInsertText(recognizedText) }) {
                Text("Insert: $recognizedText", color = Color(0xFF4285F4), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TranslatePanel(
    onInsertText: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var translatedText by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            placeholder = { Text("Enter text", color = Color.Gray, fontSize = 12.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4285F4),
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                // Placeholder for translation
                translatedText = "[Translation: $inputText]"
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Translate", fontSize = 12.sp)
        }
        
        if (translatedText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(translatedText, color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(onClick = { onInsertText(translatedText) }) {
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
        listOf("😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂"),
        listOf("❤️", "🔥", "✨", "👍", "👎", "👏", "🎉", "💯"),
        listOf("🥰", "😍", "🤩", "😘", "😊", "🙂", "😎", "🤔")
    )
    
    Column(
        modifier = Modifier.fillMaxSize(),
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
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { onEmojiClick(emoji) }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
