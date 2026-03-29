package com.aikeyboard.feature.keyboard.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme

/** Top toolbar with 6 action buttons — emoji, clipboard, credentials, language, settings, voice. */
@Composable
fun ToolbarRow(
    onEmojiClick    : () -> Unit,
    onClipClick     : () -> Unit,
    onCredClick     : () -> Unit,
    onLangClick     : () -> Unit,
    onSettingsClick : () -> Unit,
    onVoiceClick    : () -> Unit,
    modifier        : Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ToolbarBtn(icon = Icons.Outlined.EmojiEmotions, onClick = onEmojiClick)
        ToolbarBtn(icon = Icons.Outlined.ContentPaste,  onClick = onClipClick)
        ToolbarBtn(icon = Icons.Outlined.Key,           onClick = onCredClick)
        ToolbarBtn(icon = Icons.Outlined.Language,      onClick = onLangClick)
        ToolbarBtn(icon = Icons.Outlined.Settings,      onClick = onSettingsClick)
        ToolbarBtn(icon = Icons.Outlined.Mic,           onClick = onVoiceClick)
    }
}

@Composable
private fun ToolbarBtn(icon: ImageVector, onClick: () -> Unit) {
    val colors            = KbTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = interactionSource,
                       indication = rememberRipple(color = colors.ripple),
                       onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = null,
             tint = colors.keyIcon, modifier = Modifier.size(22.dp))
    }
}
