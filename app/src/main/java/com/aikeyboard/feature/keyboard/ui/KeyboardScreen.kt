package com.aikeyboard.feature.keyboard.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.PixelProKeyboardTheme
import com.aikeyboard.feature.clipboard.ui.ClipboardOverlay
import com.aikeyboard.feature.credentials.ui.CredentialsOverlay
import com.aikeyboard.feature.emoji.ui.EmojiOverlay
import com.aikeyboard.feature.keyboard.domain.model.OverlayType
import com.aikeyboard.feature.keyboard.ui.components.*
import com.aikeyboard.feature.settings.ui.SettingsOverlay

/**
 * Root composable for the full keyboard surface.
 * Reads state from [KeyboardViewModel] and delegates to child composables.
 * No logic lives here — only composition.
 */
@Composable
fun KeyboardScreen(viewModel: KeyboardViewModel) {
    val state          by viewModel.state.collectAsState()
    val clipboardItems by viewModel.clipboardItems.collectAsState()
    val credentials    by viewModel.credentials.collectAsState()

    PixelProKeyboardTheme(isDark = state.isDarkTheme) {
        val colors = KbTheme.colors

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(colors.kbSurface),
        ) {

            // ── Main keyboard body ────────────────────────────────────────────
            Column(modifier = Modifier.fillMaxWidth()) {

                // Top area: toolbar + suggestion bar (hidden while voice is active)
                AnimatedVisibility(
                    visible = !state.isVoiceActive,
                    enter   = fadeIn(),
                    exit    = fadeOut(),
                ) {
                    Column {
                        ToolbarRow(
                            onEmojiClick    = { viewModel.showOverlay(OverlayType.EMOJI) },
                            onClipClick     = { viewModel.showOverlay(OverlayType.CLIPBOARD) },
                            onCredClick     = { viewModel.showOverlay(OverlayType.CREDENTIALS) },
                            onLangClick     = { viewModel.swapLanguage() },
                            onSettingsClick = { viewModel.showOverlay(OverlayType.SETTINGS) },
                            onVoiceClick    = { viewModel.startVoice() },
                        )
                        SuggestionBar(
                            suggestions = state.suggestions,
                            onWordClick = { viewModel.usePrediction(it) },
                        )
                    }
                }

                // Inline voice bar (shown while voice is active)
                AnimatedVisibility(
                    visible = state.isVoiceActive,
                    enter   = expandVertically() + fadeIn(),
                    exit    = shrinkVertically() + fadeOut(),
                ) {
                    VoiceBar(
                        voiceLang      = state.voiceLanguage,
                        voiceEngine    = state.voiceEngine,
                        voiceStatus    = state.voiceStatus,
                        onLangToggle   = { viewModel.toggleVoiceLanguage() },
                        onEngineToggle = { viewModel.toggleVoiceEngine() },
                        onStop         = { viewModel.stopVoice() },
                    )
                }

                // Key grid
                KeyGrid(
                    language     = state.language,
                    mode         = state.mode,
                    isCaps       = state.isCaps,
                    onChar       = { viewModel.insert(it) },
                    onBackspace  = { viewModel.backspace() },
                    onShift      = { viewModel.toggleCaps() },
                    onModeToggle = { viewModel.toggleMode() },
                    onComma      = { viewModel.insert(",") },
                    onPeriod     = { viewModel.insert(".") },
                    onSpace      = { viewModel.insert(" ") },
                    onEnter      = { viewModel.insert("\n") },
                )
            }

            // ── Overlays (slide up over the keyboard) ─────────────────────────
            val slideSpec = tween<IntOffset>(durationMillis = 300)

            AnimatedVisibility(
                visible  = state.activeOverlay == OverlayType.EMOJI,
                enter    = slideInVertically(animationSpec = slideSpec) { it } + fadeIn(),
                exit     = slideOutVertically(animationSpec = slideSpec) { it } + fadeOut(),
                modifier = Modifier.matchParentSize(),
            ) {
                EmojiOverlay(
                    onBack  = { viewModel.hideOverlay() },
                    onEmoji = { viewModel.insert(it) },
                )
            }

            AnimatedVisibility(
                visible  = state.activeOverlay == OverlayType.CLIPBOARD,
                enter    = slideInVertically(animationSpec = slideSpec) { it } + fadeIn(),
                exit     = slideOutVertically(animationSpec = slideSpec) { it } + fadeOut(),
                modifier = Modifier.matchParentSize(),
            ) {
                ClipboardOverlay(
                    items   = clipboardItems,
                    onBack  = { viewModel.hideOverlay() },
                    onPaste = { viewModel.pasteFromClipboard(it) },
                    onSave  = { viewModel.saveClipToCredentials(it) },
                )
            }

            AnimatedVisibility(
                visible  = state.activeOverlay == OverlayType.CREDENTIALS || state.showAddCredential,
                enter    = slideInVertically(animationSpec = slideSpec) { it } + fadeIn(),
                exit     = slideOutVertically(animationSpec = slideSpec) { it } + fadeOut(),
                modifier = Modifier.matchParentSize(),
            ) {
                CredentialsOverlay(
                    items              = credentials,
                    onBack             = { viewModel.hideOverlay() },
                    onPaste            = { viewModel.pasteFromCredential(it) },
                    showAddDialog      = state.showAddCredential,
                    newCredentialName  = state.newCredentialName,
                    newCredentialValue = state.newCredentialValue,
                    onShowAddDialog    = { viewModel.showAddCredentialDialog() },
                    onHideAddDialog    = { viewModel.hideAddCredentialDialog() },
                    onNameChange       = { viewModel.setCredentialName(it) },
                    onValueChange      = { viewModel.setCredentialValue(it) },
                    onSaveCredential   = { viewModel.saveNewCredential() },
                )
            }

            AnimatedVisibility(
                visible  = state.activeOverlay == OverlayType.SETTINGS,
                enter    = slideInVertically(animationSpec = slideSpec) { it } + fadeIn(),
                exit     = slideOutVertically(animationSpec = slideSpec) { it } + fadeOut(),
                modifier = Modifier.matchParentSize(),
            ) {
                SettingsOverlay(
                    isDarkTheme    = state.isDarkTheme,
                    isHaptic       = state.isHapticEnabled,
                    onBack         = { viewModel.hideOverlay() },
                    onDarkToggle   = { viewModel.toggleDarkTheme() },
                    onHapticToggle = { viewModel.toggleHaptic() },
                )
            }
        }
    }
}
