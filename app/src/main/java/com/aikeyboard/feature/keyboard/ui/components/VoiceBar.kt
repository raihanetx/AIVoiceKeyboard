package com.aikeyboard.feature.keyboard.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType
import com.aikeyboard.core.theme.Palette
import com.aikeyboard.feature.keyboard.domain.model.KeyboardLanguage

/** Inline voice bar: language pill + animated 5-bar equalizer + red stop button. */
@Composable
fun VoiceBar(
    voiceLang    : KeyboardLanguage,
    onLangToggle : () -> Unit,
    onStop       : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    val colors    = KbTheme.colors
    val langLabel = if (voiceLang == KeyboardLanguage.EN) "English" else "বাংলা"

    Row(
        modifier              = modifier.fillMaxWidth().height(48.dp).padding(horizontal = 16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Language pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.specialBg)
                .clickable(onClick = onLangToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = Icons.Outlined.Language, contentDescription = null,
                 tint = colors.keyText, modifier = Modifier.size(14.dp))
            Text(text = langLabel, style = KbType.voiceLang, color = colors.keyText)
        }

        // Animated equalizer (5 bars)
        Spacer(Modifier.weight(1f))
        AudioVisualizer(accentColor = colors.accent, modifier = Modifier.weight(1f))
        Spacer(Modifier.weight(1f))

        // Stop button
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape)
                .background(Palette.ErrorRed).clickable(onClick = onStop),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = Icons.Outlined.Stop, contentDescription = "Stop",
                 tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

private val barDelays = listOf(0, 200, 400, 100, 300)

@Composable
private fun AudioVisualizer(accentColor: Color, modifier: Modifier) {
    Row(modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically) {
        repeat(5) { i -> AnimatedBar(accentColor, barDelays[i]) }
    }
}

@Composable
private fun AnimatedBar(accentColor: Color, delayMs: Int) {
    val transition = rememberInfiniteTransition(label = "bar$delayMs")
    val height by transition.animateFloat(
        initialValue   = 6f, targetValue = 28f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ), label = "h",
    )
    val alpha by transition.animateFloat(
        initialValue  = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMs),
        ), label = "a",
    )
    Box(modifier = Modifier.width(3.dp).height(height.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(accentColor.copy(alpha = alpha)))
}
