package com.aikeyboard.feature.credentials.domain.model

enum class CredIconType { LOCK, PHONE, EMAIL }

data class CredentialEntry(
    val id       : String,
    val text     : String,
    val label    : String,
    val iconType : CredIconType = CredIconType.LOCK,
)
