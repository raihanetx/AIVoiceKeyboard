package com.aikeyboard.feature.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.core.theme.KbType

// ─────────────────────────────────────────────────────────────────────────────
// OverlayHeader  — shared by Clipboard, Credentials, Settings, Emoji
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun OverlayHeader(title: String, onBack: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier         = Modifier.fillMaxWidth().height(52.dp).background(colors.kbSurface),
        contentAlignment = Alignment.Center,
    ) {
        // Centered title
        Text(text = title, style = KbType.overlayTitle, color = colors.keyText)

        // Back button — left-anchored
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.ArrowBack,
                contentDescription = "Back",
                tint               = colors.accent,
                modifier           = Modifier.size(20.dp),
            )
            Text(text = "Back", style = KbType.navBack, color = colors.accent)
        }

        // Bottom divider line
        HorizontalDivider(
            color     = colors.divider,
            thickness = 1.dp,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ListTile  — shared by Clipboard and Credentials overlays
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ListTile(
    icon            : ImageVector,
    title           : String,
    subtitle        : String,
    onClick         : () -> Unit,
    modifier        : Modifier = Modifier,
    iconTint        : Color? = null,
    iconBg          : Color? = null,
    trailingContent : @Composable (() -> Unit)? = null,
) {
    val colors            = KbTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.keyBg)
            .clickable(
                interactionSource = interactionSource,
                indication        = rememberRipple(color = colors.ripple),
                onClick           = onClick,
            )
            .padding(14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Leading icon box
        Box(
            modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                .background(iconBg ?: colors.specialBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint ?: colors.keyIcon,
                modifier           = Modifier.size(20.dp),
            )
        }

        // Title + subtitle
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title,    style = KbType.tileText, color = colors.keyText,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = KbType.tileSub,  color = colors.keyIcon)
        }

        // Optional trailing widget (e.g. "Save" button)
        trailingContent?.invoke()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TileActionButton  — "Save" button inside clipboard tiles
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TileActionButton(label: String, onClick: () -> Unit) {
    val colors = KbTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(colors.specialBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = KbType.tileSub, color = colors.keyText)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SettingsGroupTitle  — blue uppercase section header in Settings
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SettingsGroupTitle(title: String) {
    val colors = KbTheme.colors
    Text(
        text     = title.uppercase(),
        style    = KbType.groupTitle,
        color    = colors.accent,
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
    )
}
