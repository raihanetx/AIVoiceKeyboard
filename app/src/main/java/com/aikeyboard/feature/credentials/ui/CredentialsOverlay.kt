package com.aikeyboard.feature.credentials.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aikeyboard.core.theme.KbTheme
import com.aikeyboard.feature.credentials.domain.model.CredIconType
import com.aikeyboard.feature.credentials.domain.model.CredentialEntry
import com.aikeyboard.feature.shared.ui.ListTile
import com.aikeyboard.feature.shared.ui.OverlayHeader

@Composable
fun CredentialsOverlay(
    items   : List<CredentialEntry>,
    onBack  : () -> Unit,
    onPaste : (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = KbTheme.colors
    Column(modifier = modifier.fillMaxSize().background(colors.kbSurface)) {
        OverlayHeader(title = "Credentials", onBack = onBack)
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items, key = { it.id }) { entry ->
                ListTile(
                    icon     = when (entry.iconType) {
                        CredIconType.PHONE -> Icons.Outlined.Phone
                        CredIconType.EMAIL -> Icons.Outlined.Email
                        else               -> Icons.Outlined.Lock
                    },
                    // Accent tint for secure tiles (matches HTML .secure style)
                    iconTint = colors.accent,
                    iconBg   = colors.accentSoft,
                    title    = entry.text,
                    subtitle = entry.label,
                    onClick  = { onPaste(entry.text) },
                )
            }
        }
    }
}
