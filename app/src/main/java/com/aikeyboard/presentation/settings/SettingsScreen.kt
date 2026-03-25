package com.aikeyboard.presentation.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aikeyboard.core.constants.AppConstants
import com.aikeyboard.domain.model.Language
import com.aikeyboard.presentation.theme.AIVoiceKeyboardTheme
import com.aikeyboard.presentation.theme.Color

/**
 * Settings screen composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AIVoiceKeyboardTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.OnBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Language Selection
            SettingsSection(title = "Language") {
                LanguageSelector(
                    selectedLanguage = uiState.selectedLanguage,
                    onLanguageSelected = { viewModel.setLanguage(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STT Engine Selection
            SettingsSection(title = "Speech-to-Text Engine") {
                SttEngineSelector(
                    selectedEngine = uiState.sttEngine,
                    onEngineSelected = { viewModel.setSttEngine(it) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            SettingsSection(title = "About") {
                AboutCard()
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Reset Button
            OutlinedButton(
                onClick = { viewModel.resetSettings() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Defaults")
            }
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color.TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.CardBackground),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun LanguageSelector(
    selectedLanguage: Language,
    onLanguageSelected: (Language) -> Unit
) {
    Column {
        Language.entries.forEach { language ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedLanguage == language,
                    onClick = { onLanguageSelected(language) },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.Primary)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "${language.displayName} (${language.nativeName})",
                    color = Color.OnBackground,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun SttEngineSelector(
    selectedEngine: String,
    onEngineSelected: (String) -> Unit
) {
    Column {
        SttEngineOption(
            engine = AppConstants.STT_ENGINE_ANDROID,
            title = "Android (Offline)",
            description = "Built-in speech recognition, works offline",
            isSelected = selectedEngine == AppConstants.STT_ENGINE_ANDROID,
            onSelect = { onEngineSelected(AppConstants.STT_ENGINE_ANDROID) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        SttEngineOption(
            engine = AppConstants.STT_ENGINE_GROQ,
            title = "Groq Whisper (Online)",
            description = "High accuracy AI transcription, requires internet",
            isSelected = selectedEngine == AppConstants.STT_ENGINE_GROQ,
            onSelect = { onEngineSelected(AppConstants.STT_ENGINE_GROQ) }
        )
    }
}

@Composable
fun SttEngineOption(
    engine: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.Primary.copy(alpha = 0.1f) else Color.Surface
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = Color.Primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.OnBackground,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = Color.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun AboutCard() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color.Primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AI Voice Keyboard",
                    color = Color.OnBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Version 3.0.0",
                    color = Color.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "A smart keyboard with AI-powered voice input and translation features. " +
                   "Supports English and Bengali languages with both offline and online " +
                   "speech recognition options.",
            color = Color.TextSecondary,
            fontSize = 14.sp
        )
    }
}
