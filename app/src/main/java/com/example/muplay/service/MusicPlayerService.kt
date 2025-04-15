package com.example.muplay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.muplay.R
import com.example.muplay.data.model.LrcContent
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.LyricLine
import com.example.muplay.data.repository.PlayCountRepository
import com.example.muplay.data.repository.LyricRepository
import com.example.muplay.presentation.MainActivity
import com.example.muplay.receiver.MediaButtonReceiver
import com.example.muplay.util.Constants
import com.example.muplay.util.NotificationPermissionHelper
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "MusicPlayerService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "music_playback_channel"

@AndroidEntryPoint
class MusicPlayerService : Service() {
    // Custom CoroutineScope untuk service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var playCountRepository: PlayCountRepository

    @Inject
    lateinit var lyricRepository: LyricRepository

    private val binder = MusicPlayerBinder()
    private lateinit var player: ExoPlayer
    private var positionPollerJob: Job? = null
    private var isServiceActive = true

    // Inisialisasi CoverArtManager
    private lateinit var coverArtManager: CoverArtManager

    private val _currentMusic = MutableStateFlow<Music?>(null)
    val currentMusic: StateFlow<Music?> = _currentMusic.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _playlist = MutableStateFlow<List<Music>>(emptyList())
    val playlist: StateFlow<List<Music>> = _playlist.asStateFlow()

    // Lyrics state for the current track
    private val _currentLrcContent = MutableStateFlow<LrcContent>(LrcContent())
    val currentLrcContent: StateFlow<LrcContent> = _currentLrcContent.asStateFlow()

    private val _currentLyricLine = MutableStateFlow<LyricLine?>(null)
    val currentLyricLine: StateFlow<LyricLine?> = _currentLyricLine.asStateFlow()

    // Define playback states
    companion object {
        const val STATE_PLAYING = 3
        const val STATE_PAUSED = 2

        // Threshold for frequently played songs
        const val PLAY_COUNT_THRESHOLD = 5
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Playing state changed: $isPlaying")
            _isPlaying.value = isPlaying

            // Start or stop position poller based on playing state
            if (isPlaying) {
                startPositionPoller()
                safelyUpdateNotification()

                // Increment play count when playback starts
                _currentMusic.value?.let { music ->
                    Log.d(TAG, "Incrementing play count as playback started: ${music.id}")
                    incrementPlayCount(music.id)
                }
            } else {
                stopPositionPoller()
                safelyUpdateNotification()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "Playback state changed: $playbackState")
            if (playbackState == Player.STATE_ENDED) {
                // Increment play count when the song finishes playing
                _currentMusic.value?.let { music ->
                    Log.d(TAG, "Incrementing play count as playback completed: ${music.id}")
                    incrementPlayCount(music.id)
                }
            } else if (playbackState == Player.STATE_READY) {
                Log.d(TAG, "Player is ready")
                safelyUpdateNotification()
            } else if (playbackState == Player.STATE_BUFFERING) {
                Log.d(TAG, "Player is buffering")
            } else if (playbackState == Player.STATE_IDLE) {
                Log.d(TAG, "Player is idle")
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            Log.d(TAG, "Media item transition: ${mediaItem?.mediaId}, reason: $reason")
            mediaItem?.let {
                // Update current song info
                updateCurrentMusic(it)
                safelyUpdateNotification()

                // Load lyrics for the new track
                loadLyricsForCurrentTrack()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
        }
    }

    /**
     * Load lyrics for the current track
     */
    private fun loadLyricsForCurrentTrack() {
        val musicId = _currentMusic.value?.id ?: return

        ioScope.launch {
            try {
                if (lyricRepository.hasLyrics(musicId)) {
                    val lrcContent = lyricRepository.loadLrcContent(musicId)
                    _currentLrcContent.value = lrcContent
                    Log.d(TAG, "Loaded lyrics for music ID $musicId with ${lrcContent.lines.size} lines")
                } else {
                    _currentLrcContent.value = LrcContent()
                    _currentLyricLine.value = null
                    Log.d(TAG, "No lyrics found for music ID $musicId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading lyrics: ${e.message}", e)
            }
        }
    }

    /**
     * Update the current lyric line based on playback position
     */
    private fun updateCurrentLyricLine(position: Long) {
        val content = _currentLrcContent.value
        if (content.lines.isEmpty()) return

        ioScope.launch {
            try {
                val currentLine = lyricRepository.getCurrentLyricLine(content, position)
                _currentLyricLine.value = currentLine
            } catch (e: Exception) {
                Log.e(TAG, "Error updating current lyric line: ${e.message}", e)
            }
        }
    }

    // Helper function to increment play count with proper error handling
    private fun incrementPlayCount(musicId: Long) {
        ioScope.launch {
            try {
                // Use IO Dispatcher for database operations
                playCountRepository.incrementPlayCount(musicId)
                Log.d(TAG, "Successfully incremented play count for music ID $musicId")

                // Small delay to ensure DB operation completes
                delay(100)
            } catch (e: Exception) {
                Log.e(TAG, "Error incrementing play count: ${e.message}", e)
            }
        }
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        try {
            // Initialize CoverArtManager
            coverArtManager = CoverArtManager(this)

            // Initialize ExoPlayer
            player = ExoPlayer.Builder(this).build().apply {
                addListener(playerListener)
            }

            // Create notification channel for API 26+
            createNotificationChannel()

            // Start service as foreground with empty notification
            startForeground(NOTIFICATION_ID, createEmptyNotification())

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player: ${e.message}", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for currently playing music"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Muplay")
            .setContentText("Music Playback")
            .setSmallIcon(R.drawable.ic_music_note)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Safely update notification - only if we have permission on Android 13+
     */
    private fun safelyUpdateNotification() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            NotificationPermissionHelper.hasNotificationPermission(this)) {
            updateNotification()
        } else {
            Log.d(TAG, "Skipping notification update - no permission")
        }
    }

    private fun updateNotification() {
        val music = _currentMusic.value ?: return

        // Load album art for notification
        try {
            if (music.albumArtPath != null) {
                serviceScope.launch(Dispatchers.IO) {
                    // Use CoverArtManager to load album art
                    val bitmap = coverArtManager.loadCoverArtBitmap(music.albumArtPath)
                    if (bitmap != null) {
                        createAndShowNotification(music, bitmap)
                    } else {
                        // If loading fails, show notification without album art
                        createAndShowNotification(music, null)
                    }
                }
            } else {
                // If no album art, show notification without it
                createAndShowNotification(music, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateNotification: ${e.message}", e)
            // If an error occurs, still show notification without album art
            createAndShowNotification(music, null)
        }
    }

    private fun createAndShowNotification(music: Music, albumArt: Bitmap?) {
        // Create play/pause action
        val playPauseAction = NotificationCompat.Action.Builder(
            if (_isPlaying.value) R.drawable.ic_pause else R.drawable.ic_play,
            if (_isPlaying.value) "Pause" else "Play",
            createMediaButtonPendingIntent(MediaButtonReceiver.ACTION_PLAY_PAUSE)
        ).build()

        // Create previous action
        val prevAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_previous,
            "Previous",
            createMediaButtonPendingIntent(MediaButtonReceiver.ACTION_SKIP_TO_PREVIOUS)
        ).build()

        // Create next action
        val nextAction = NotificationCompat.Action.Builder(
            R.drawable.ic_skip_next,
            "Next",
            createMediaButtonPendingIntent(MediaButtonReceiver.ACTION_SKIP_TO_NEXT)
        ).build()

        // Create content intent
        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, contentIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // Create notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(music.title)
            .setContentText(music.artist)
            .setSubText(music.album)
            .setLargeIcon(albumArt)
            .setSmallIcon(R.drawable.ic_music_note)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(createMediaButtonPendingIntent(MediaButtonReceiver.ACTION_STOP))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(prevAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()

        try {
            // Update notification
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification: ${e.message}", e)
        }
    }

    private fun createMediaButtonPendingIntent(action: Long): PendingIntent {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON)
        intent.setPackage(packageName)
        intent.putExtra("action", action)

        return PendingIntent.getBroadcast(
            this, action.toInt(), intent, PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service start command received")

        // Handle media button actions
        intent?.getLongExtra("action", -1)?.let { actionId ->
            when (actionId) {
                MediaButtonReceiver.ACTION_PLAY_PAUSE -> togglePlayPause()
                MediaButtonReceiver.ACTION_SKIP_TO_NEXT -> skipToNext()
                MediaButtonReceiver.ACTION_SKIP_TO_PREVIOUS -> skipToPrevious()
                MediaButtonReceiver.ACTION_STOP -> stopSelf()
            }
        }

        // If service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "Service being destroyed")
        isServiceActive = false
        stopPositionPoller()

        try {
            player.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources: ${e.message}", e)
        }

        // Cancel all coroutines
        serviceScope.cancel()
        ioScope.cancel()

        super.onDestroy()
    }

    private fun startPositionPoller() {
        // Cancel existing job if any
        stopPositionPoller()

        // Start new poller
        positionPollerJob = serviceScope.launch {
            try {
                while (isActive && isServiceActive && _isPlaying.value) {
                    val position = player.currentPosition
                    _playbackPosition.value = position
                    // Update lyric line if we have lyrics
                    if (_currentLrcContent.value.lines.isNotEmpty()) {
                        updateCurrentLyricLine(position)
                    }
                    kotlinx.coroutines.delay(200) // Update more frequently for smoother lyrics sync
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in position poller: ${e.message}", e)
            }
        }
    }

    private fun stopPositionPoller() {
        positionPollerJob?.cancel()
        positionPollerJob = null
    }

    private fun updateCurrentMusic(mediaItem: MediaItem) {
        try {
            val musicId = mediaItem.mediaId.toLongOrNull() ?: return
            Log.d(TAG, "Updating current music with ID: $musicId")

            // Get song info from extras
            val extras = mediaItem.mediaMetadata.extras
            extras?.let { bundle ->
                val music = bundle.getMusicFromExtras()
                _currentMusic.value = music
                Log.d(TAG, "Current music updated to: ${music.title}")

                // Update notification
                safelyUpdateNotification()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current music: ${e.message}", e)
        }
    }

    fun playMusic(music: Music, playlistSongs: List<Music> = emptyList()) {
        try {
            Log.d(TAG, "Playing music: ${music.id} - ${music.title}")
            Log.d(TAG, "Music URI: ${music.uri}")

            // Set playlist if available
            if (playlistSongs.isNotEmpty()) {
                setPlaylist(playlistSongs, music)
            } else {
                // Play single song
                val mediaItem = createMediaItem(music)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                _currentMusic.value = music
                _isPlaying.value = true

                Log.d(TAG, "Started playing music: ${music.title}")

                // Increment play count when explicitly playing a song
                incrementPlayCount(music.id)

                // Load lyrics for the current track
                loadLyricsForCurrentTrack()

                // Update notification
                safelyUpdateNotification()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music: ${e.message}", e)
        }
    }

    fun updateCurrentMusicCoverArt(updatedMusic: Music) {
        try {
            // Update current music with new cover art
            _currentMusic.value = updatedMusic

            // Update notification with new cover art
            safelyUpdateNotification()

            // Log to confirm the change
            Log.d(TAG, "Updated cover art: ${updatedMusic.albumArtPath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current music cover art: ${e.message}", e)
        }
    }

    fun updateCurrentMusicMetadata(updatedMusic: Music) {
        try {
            // Update current music with new metadata
            _currentMusic.value = updatedMusic

            // Update notification with new metadata
            safelyUpdateNotification()

            Log.d(TAG, "Updated metadata in service: ${updatedMusic.title} by ${updatedMusic.artist}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current music metadata: ${e.message}", e)
        }
    }

    fun setPlaylist(songs: List<Music>, currentMusic: Music? = null) {
        try {
            Log.d(TAG, "Setting playlist with ${songs.size} songs")
            _playlist.value = songs

            // Reset player and add all songs to playlist
            player.clearMediaItems()

            val mediaItems = songs.map { createMediaItem(it) }
            player.addMediaItems(mediaItems)
            player.prepare()

            // If there's a specific song to play, find its position
            if (currentMusic != null) {
                val index = songs.indexOfFirst { it.id == currentMusic.id }
                if (index != -1) {
                    player.seekTo(index, 0)
                    Log.d(TAG, "Seeking to song at position $index: ${currentMusic.title}")

                    // Increment play count for this song
                    incrementPlayCount(currentMusic.id)

                    // Load lyrics for this track
                    loadLyricsForCurrentTrack()
                }
            }

            player.play()

            // Update notification
            safelyUpdateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting playlist: ${e.message}", e)
        }
    }

    private fun createMediaItem(music: Music): MediaItem {
        Log.d(TAG, "Creating media item for: ${music.title}")
        val extras = Bundle().apply {
            putLong(Constants.EXTRA_MUSIC_ID, music.id)
            putString(Constants.EXTRA_MUSIC_TITLE, music.title)
            putString(Constants.EXTRA_MUSIC_ARTIST, music.artist)
            putString(Constants.EXTRA_MUSIC_ALBUM, music.album)
            putLong(Constants.EXTRA_MUSIC_DURATION, music.duration)
            putString(Constants.EXTRA_MUSIC_ART_URI, music.albumArtPath)
            putString(Constants.EXTRA_MUSIC_URI, music.uri)
        }

        return MediaItem.Builder()
            .setUri(Uri.parse(music.uri))
            .setMediaId(music.id.toString())
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(music.title)
                    .setArtist(music.artist)
                    .setAlbumTitle(music.album)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    fun togglePlayPause() {
        try {
            Log.d(TAG, "Toggle play/pause. Currently playing: ${player.isPlaying}")
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()

                // Increment play count when resuming play
                _currentMusic.value?.let { music ->
                    incrementPlayCount(music.id)
                }
            }
            _isPlaying.value = player.isPlaying
            safelyUpdateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause: ${e.message}", e)
        }
    }

    fun skipToNext() {
        try {
            Log.d(TAG, "Skip to next")
            if (player.hasNextMediaItem()) {
                player.seekToNext()
                player.playWhenReady = true  // Ensure it plays after skipping
                Log.d(TAG, "Skipped to next media item. Current index: ${player.currentMediaItemIndex}")
            } else {
                // If at the last song, go back to the first one (wrap around)
                if (player.mediaItemCount > 0) {
                    player.seekTo(0, 0)
                    player.playWhenReady = true
                    Log.d(TAG, "At last item, wrapped around to first track (index 0)")
                }
            }
            safelyUpdateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in skipToNext: ${e.message}", e)
        }
    }

    fun skipToPrevious() {
        try {
            Log.d(TAG, "Skip to previous")
            // If we're more than 3 seconds into the song, restart it instead of going to previous
            if (player.currentPosition > 3000) {
                player.seekTo(0)
                player.playWhenReady = true
                Log.d(TAG, "Restarting current track because position > 3s")
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
                player.playWhenReady = true  // Ensure it plays after skipping
                Log.d(TAG, "Skipped to previous media item. Current index: ${player.currentMediaItemIndex}")
            } else {
                // If at the first song, go to the last one (wrap around)
                if (player.mediaItemCount > 0) {
                    player.seekTo(player.mediaItemCount - 1, 0)
                    player.playWhenReady = true
                    Log.d(TAG, "At first item, wrapped around to last track (index ${player.mediaItemCount - 1})")
                } else {
                    // If no previous song, go back to the start of current song
                    player.seekTo(0)
                    Log.d(TAG, "No tracks available, restarting current track")
                }
            }
            safelyUpdateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error in skipToPrevious: ${e.message}", e)
        }
    }

    fun seekTo(position: Long) {
        try {
            Log.d(TAG, "Seek to position: $position")
            player.seekTo(position)
            _playbackPosition.value = position

            // Update lyric line based on new position
            if (_currentLrcContent.value.lines.isNotEmpty()) {
                updateCurrentLyricLine(position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in seekTo: ${e.message}", e)
        }
    }

    fun toggleShuffleMode() {
        try {
            val newMode = !player.shuffleModeEnabled
            Log.d(TAG, "Toggling shuffle mode to: $newMode")
            player.shuffleModeEnabled = newMode
            _shuffleMode.value = newMode
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleShuffleMode: ${e.message}", e)
        }
    }

    fun toggleRepeatMode() {
        try {
            val currentMode = player.repeatMode
            val newMode = when (currentMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
            Log.d(TAG, "Toggling repeat mode from $currentMode to $newMode")
            player.repeatMode = newMode
            _repeatMode.value = newMode
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleRepeatMode: ${e.message}", e)
        }
    }

    fun setVolume(volume: Float) {
        try {
            Log.d(TAG, "Setting volume to: $volume")
            player.volume = volume
        } catch (e: Exception) {
            Log.e(TAG, "Error in setVolume: ${e.message}", e)
        }
    }
}

// Extension functions
private fun Bundle.getMusicFromExtras(): Music {
    return Music(
        id = getLong(Constants.EXTRA_MUSIC_ID),
        title = getString(Constants.EXTRA_MUSIC_TITLE, ""),
        artist = getString(Constants.EXTRA_MUSIC_ARTIST, ""),
        album = getString(Constants.EXTRA_MUSIC_ALBUM, ""),
        duration = getLong(Constants.EXTRA_MUSIC_DURATION),
        uri = getString(Constants.EXTRA_MUSIC_URI, ""),
        albumArtPath = getString(Constants.EXTRA_MUSIC_ART_URI)
    )
}