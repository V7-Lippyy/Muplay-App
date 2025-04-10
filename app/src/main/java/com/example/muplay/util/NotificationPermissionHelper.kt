package com.example.muplay.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Helper class to manage notification permissions for Android 13+
 */
class NotificationPermissionHelper(private val activity: ComponentActivity) {

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    /**
     * Initialize the permission launcher
     */
    fun setupPermissionLauncher() {
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, we can now send notifications
                // No additional action needed as services will now work properly
            } else {
                // Permission denied - we could show a message to the user explaining
                // the importance of notifications for a music player app
            }
        }
    }

    /**
     * Check and request notification permission if needed
     */
    fun checkAndRequestNotificationPermission() {
        // Only needed on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted, nothing to do
                }
                activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // We should show a rationale before requesting
                    // For simplicity, we're just requesting directly, but you could
                    // show a dialog explaining why notifications are important first
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // First time asking or user selected "Don't ask again" but we need to ask
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    companion object {
        /**
         * Utility method to check if notification permission is granted
         */
        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                // For versions before Android 13, permission is granted at install time
                true
            }
        }
    }
}