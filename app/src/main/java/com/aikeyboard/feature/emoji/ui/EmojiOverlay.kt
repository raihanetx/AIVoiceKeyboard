package com.aikeyboard.feature.emoji.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.feature.emoji.data.source.EmojiSource
import com.aikeyboard.feature.shared.ui.OverlayHeader

@Composable
fun EmojiOverlay(
    onBack   : () -> Unit,
    onEmoji  : (String) -> Unit,
    modifier : Modifier = Modifier,
) {
    val colors = KbTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.kbSurface)) {
        OverlayHeader(title = "Emoji", onBack = onBack)
        LazyVerticalGrid(
            columns               = GridCells.Adaptive(minSize = 44.dp),
            modifier              = Modifier.fillMaxSize(),
            contentPadding        = PaddingValues(12.dp),
            verticalArrangement   = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(EmojiSource.emojis) { emoji ->
                EmojiButton(emoji = emoji, onClick = { onEmoji(emoji) })
            }
        }
    }
}

@Composable
private fun EmojiButton(emoji: String, onClick: () -> Unit) {
    val colors            = KbTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = colors.ripple),
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, fontSize = 28.sp, textAlign = TextAlign.Center)
    }
}
