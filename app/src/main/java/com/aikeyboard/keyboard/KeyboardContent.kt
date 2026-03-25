package com.aikeyboard.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun KeyboardContent() {
    var currentPanel by remember { mutableStateOf("keyboard") }
    var currentLanguage by remember { mutableStateOf("en") }

    Column(
        modifier = Modifier.fillMaxWidth().height(280.dp).background(Color(0xFF1A1A2E))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("ABC" to "keyboard", "🎤" to "voice", "🌐" to "translate", "📋" to "clipboard", "😀" to "emoji").forEach { (text, panel) ->
                TextButton(onClick = { currentPanel = panel }) {
                    Text(text, color = if (currentPanel == panel) Color(0xFF4285F4) else Color.White)
                }
            }
        }
        HorizontalDivider(color = Color(0xFF333333))
        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
            when (currentPanel) {
                "keyboard" -> if (currentLanguage == "en") EnglishKeyboard() else BanglaKeyboard()
                "voice" -> Text("🎤 Voice Panel", color = Color.White)
                "translate" -> Text("🌐 Translation Panel", color = Color.White)
                "clipboard" -> Text("📋 Clipboard", color = Color.White)
                "emoji" -> Text("😀 Emoji Panel", color = Color.White)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            TextButton(onClick = { currentLanguage = "en" }) {
                Text("EN", color = if (currentLanguage == "en") Color(0xFF4285F4) else Color.Gray)
            }
            TextButton(onClick = { currentLanguage = "bn" }) {
                Text("বাং", color = if (currentLanguage == "bn") Color(0xFF4285F4) else Color.Gray)
            }
        }
    }
}
