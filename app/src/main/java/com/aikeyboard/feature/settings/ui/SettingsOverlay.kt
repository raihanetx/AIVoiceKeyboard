package com.aikeyboard.feature.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType
import com.aikeyboard.feature.settings.domain.model.VoiceEngine
import com.aikeyboard.feature.shared.ui.OverlayHeader
import com.aikeyboard.feature.shared.ui.SettingsGroupTitle

@Composable
fun SettingsOverlay(
    isDarkTheme    : Boolean,
    isHaptic       : Boolean,
    onBack         : () -> Unit,
    onDarkToggle   : () -> Unit,
    onHapticToggle : () -> Unit,
    modifier       : Modifier = Modifier,
) {
    val colors = KbTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.kbSurface)) {
        OverlayHeader(title = "Settings", onBack = onBack)
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            item {
                SettingsGroupTitle("Preferences")
                Spacer(Modifier.height(4.dp))
                SettingsToggleRow(label = "Dark Mode",       checked = isDarkTheme, onToggle = onDarkToggle)
                Spacer(Modifier.height(8.dp))
                SettingsToggleRow(label = "Haptic Feedback", checked = isHaptic,    onToggle = onHapticToggle)
            }
            item {
                SettingsGroupTitle("Voice Input")
                Spacer(Modifier.height(4.dp))
                SettingsSelectRow(label = "English Engine")
                Spacer(Modifier.height(8.dp))
                SettingsSelectRow(label = "Bengali Engine")
            }
        }
    }
}

// ── Reusable setting rows ─────────────────────────────────────────────────────

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    val colors = KbTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keyBg)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = KbType.tileText, color = colors.keyText)
        Switch(
            checked         = checked,
            onCheckedChange = { onToggle() },
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = colors.accentText,
                checkedTrackColor   = colors.accent,
                uncheckedThumbColor = colors.accentText,
                uncheckedTrackColor = colors.divider,
            ),
        )
    }
}

@Composable
private fun SettingsSelectRow(label: String) {
    val colors  = KbTheme.colors
    val options = VoiceEngine.entries.map { it.label }
    var selected by remember { mutableStateOf(options.first()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keyBg)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = KbType.tileText, color = colors.keyText)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(colors.specialBg)
                .clickable {
                    // Cycle through options on click
                    val idx = options.indexOf(selected)
                    selected = options[(idx + 1) % options.size]
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(text = selected, style = KbType.tileSub, color = colors.keyText)
        }
    }
}
