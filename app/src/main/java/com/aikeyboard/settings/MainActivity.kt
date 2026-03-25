package com.aikeyboard.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aikeyboard.ui.theme.AIVoiceKeyboardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AIVoiceKeyboardTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF1A1A2E)) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Text("AI Voice Keyboard", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Enable keyboard: Settings → System → Languages → Keyboard", color = Color.Gray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Features:", color = Color.White)
                        Text("• Voice Recognition (Gemini)", color = Color.Gray)
                        Text("• Translation (GLM-4.7-Flash)", color = Color.Gray)
                        Text("• English & Bengali", color = Color.Gray)
                    }
                }
            }
        }
    }
}
