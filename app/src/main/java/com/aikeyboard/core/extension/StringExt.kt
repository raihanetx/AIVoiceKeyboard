package com.aikeyboard.core.extension

/**
 * Extension functions for String
 */

/**
 * Check if string is blank or empty
 */
fun String?.isNotNullOrBlank(): Boolean {
    return !this.isNullOrBlank()
}

/**
 * Truncate string to specified length with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        "${this.take(maxLength)}..."
    } else {
        this
    }
}

/**
 * Safely get substring, handling index out of bounds
 */
fun String.safeSubstring(startIndex: Int, endIndex: Int): String {
    val safeStart = startIndex.coerceIn(0, this.length)
    val safeEnd = endIndex.coerceIn(safeStart, this.length)
    return this.substring(safeStart, safeEnd)
}

/**
 * Capitalize first letter
 */
fun String.capitalizeFirst(): String {
    return if (this.isNotEmpty()) {
        this[0].uppercaseChar() + this.substring(1)
    } else {
        this
    }
}

/**
 * Remove extra whitespace
 */
fun String.normalizeWhitespace(): String {
    return this.trim().replace(Regex("\\s+"), " ")
}
