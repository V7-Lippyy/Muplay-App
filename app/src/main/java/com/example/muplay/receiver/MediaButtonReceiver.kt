package com.example.muplay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.muplay.service.MusicPlayerService
import dagger.hilt.android.AndroidEntryPoint

/**
 * Broadcast receiver to handle media button events from notification or headset controls
 */
@AndroidEntryPoint
class MediaButtonReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "MediaButtonReceiver"

        // Define our own constants instead of using PlaybackStateCompat
        const val ACTION_PLAY_PAUSE = 516L
        const val ACTION_SKIP_TO_NEXT = 517L
        const val ACTION_SKIP_TO_PREVIOUS = 518L
        const val ACTION_STOP = 519L
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received media button intent: ${intent.action}")

        // Handle media button action
        val action = intent.getLongExtra("action", -1)
        if (action != -1L) {
            // Forward to service
            val serviceIntent = Intent(context, MusicPlayerService::class.java).apply {
                putExtra("action", action)
            }

            // Start the service to handle the action
            context.startService(serviceIntent)

            Log.d(TAG, "Forwarded action $action to MusicPlayerService")
        }
    }
}