// ─── domain/model ───────────────────────────────────────────────────────────
package com.aikeyboard.feature.clipboard.domain.model

enum class ClipIconType { HISTORY, LINK, TEXT }

data class ClipboardEntry(
    val id        : String,
    val text      : String,
    val timeLabel : String,
    val iconType  : ClipIconType = ClipIconType.HISTORY,
)
