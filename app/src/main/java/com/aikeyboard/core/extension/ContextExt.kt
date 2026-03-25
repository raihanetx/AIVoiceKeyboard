package com.aikeyboard.core.extension

import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.core.content.ContextCompat

/**
 * Extension functions for Context
 */

/**
 * Convert dp to pixels
 */
fun Context.dpToPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

/**
 * Convert sp to pixels
 */
fun Context.spToPx(sp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        sp.toFloat(),
        resources.displayMetrics
    ).toInt()
}

/**
 * Check if a permission is granted
 */
fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

/**
 * Check if microphone permission is granted
 */
fun Context.isMicPermissionGranted(): Boolean {
    return isPermissionGranted(android.Manifest.permission.RECORD_AUDIO)
}
