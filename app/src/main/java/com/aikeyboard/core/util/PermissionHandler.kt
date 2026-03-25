package com.aikeyboard.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aikeyboard.core.extension.isMicPermissionGranted

/**
 * Permission handling utility
 */
object PermissionHandler {

    // Required permissions
    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO
    )

    /**
     * Check if all required permissions are granted
     */
    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if microphone permission is granted
     */
    fun hasMicPermission(context: Context): Boolean {
        return context.isMicPermissionGranted()
    }

    /**
     * Get list of denied permissions
     */
    fun getDeniedPermissions(context: Context): List<String> {
        return REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check if should show permission rationale
     */
    fun shouldShowRationale(context: Context, permission: String): Boolean {
        // This should be called from an Activity context
        return if (context is android.app.Activity) {
            ActivityCompat.shouldShowRequestPermissionRationale(context, permission)
        } else {
            false
        }
    }

    /**
     * Get user-friendly permission name
     */
    fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.RECORD_AUDIO -> "Microphone"
            else -> permission.substringAfterLast(".")
        }
    }
}
