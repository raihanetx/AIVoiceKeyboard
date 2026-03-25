package com.aikeyboard.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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
                    groqKeyConfigured = uiState.groqKeyConfigured,
                    geminiKeyConfigured = uiState.geminiKeyConfigured,
                    onEngineSelected = { viewModel.setSttEngine(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // API Keys Section
            SettingsSection(title = "API Keys") {
                ApiKeyInputs(
                    groqApiKey = uiState.groqApiKey,
                    geminiApiKey = uiState.geminiApiKey,
                    groqKeyConfigured = uiState.groqKeyConfigured,
                    geminiKeyConfigured = uiState.geminiKeyConfigured,
                    onGroqKeyChange = { viewModel.setGroqApiKey(it) },
                    onGeminiKeyChange = { viewModel.setGeminiApiKey(it) }
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
    groqKeyConfigured: Boolean,
    geminiKeyConfigured: Boolean,
    onEngineSelected: (String) -> Unit
) {
    Column {
        // Android (Offline) - Always available
        SttEngineOption(
            engine = AppConstants.STT_ENGINE_ANDROID,
            title = "Android (Offline)",
            description = "Built-in speech recognition, works offline",
            isSelected = selectedEngine == AppConstants.STT_ENGINE_ANDROID,
            isAvailable = true,
            onSelect = { onEngineSelected(AppConstants.STT_ENGINE_ANDROID) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Groq (Online) - Requires API key
        SttEngineOption(
            engine = AppConstants.STT_ENGINE_GROQ,
            title = "Groq Whisper (Online)",
            description = if (groqKeyConfigured) "High accuracy AI transcription" else "⚠️ API key required",
            isSelected = selectedEngine == AppConstants.STT_ENGINE_GROQ,
            isAvailable = groqKeyConfigured,
            onSelect = { onEngineSelected(AppConstants.STT_ENGINE_GROQ) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Gemini (Live) - Requires API key
        SttEngineOption(
            engine = AppConstants.STT_ENGINE_GEMINI,
            title = "Gemini Live (Best Quality)",
            description = if (geminiKeyConfigured) "Multimodal AI, best quality" else "⚠️ API key required",
            isSelected = selectedEngine == AppConstants.STT_ENGINE_GEMINI,
            isAvailable = geminiKeyConfigured,
            onSelect = { onEngineSelected(AppConstants.STT_ENGINE_GEMINI) }
        )
    }
}

@Composable
fun SttEngineOption(
    engine: String,
    title: String,
    description: String,
    isSelected: Boolean,
    isAvailable: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color.Primary.copy(alpha = 0.1f)
                !isAvailable -> Color.Surface.copy(alpha = 0.5f)
                else -> Color.Surface
            }
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
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.Primary,
                    unselectedColor = if (isAvailable) Color.OnSurface.copy(alpha = 0.6f) else Color.OnSurface.copy(alpha = 0.3f)
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = if (isAvailable) Color.OnBackground else Color.OnBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Text(
                    text = description,
                    color = if (!isAvailable) Color.Error.copy(alpha = 0.7f) else Color.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * API Key input fields
 */
@Composable
fun ApiKeyInputs(
    groqApiKey: String,
    geminiApiKey: String,
    groqKeyConfigured: Boolean,
    geminiKeyConfigured: Boolean,
    onGroqKeyChange: (String) -> Unit,
    onGeminiKeyChange: (String) -> Unit
) {
    Column {
        // Groq API Key
        ApiKeyInputField(
            label = "Groq API Key",
            value = groqApiKey,
            isConfigured = groqKeyConfigured,
            placeholder = "gsk_xxxxxxxxxxxx",
            helperText = "Get free key from console.groq.com (~10K req/day)",
            onValueChange = onGroqKeyChange
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Gemini API Key
        ApiKeyInputField(
            label = "Gemini API Key",
            value = geminiApiKey,
            isConfigured = geminiKeyConfigured,
            placeholder = "AIzaxxxxxxxxxx",
            helperText = "Get free key from aistudio.google.com/apikey (1M tokens/day)",
            onValueChange = onGeminiKeyChange
        )
    }
}

@Composable
fun ApiKeyInputField(
    label: String,
    value: String,
    isConfigured: Boolean,
    placeholder: String,
    helperText: String,
    onValueChange: (String) -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.OnBackground,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
            if (isConfigured) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Configured",
                    tint = Color.Success,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color.TextSecondary) },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide" else "Show",
                        tint = Color.TextSecondary
                    )
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color.Primary,
                unfocusedBorderColor = Color.TextSecondary.copy(alpha = 0.3f),
                cursorColor = Color.Primary
            )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = helperText,
            color = Color.TextSecondary,
            fontSize = 11.sp
        )
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
