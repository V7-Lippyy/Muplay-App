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
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.HistoryRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "MusicPlayerService"
private const val NOTIFICATION_ID = 1001
private const val CHANNEL_ID = "music_playback_channel"

@AndroidEntryPoint
class MusicPlayerService : Service() {
    // Custom CoroutineScope untuk service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var historyRepository: HistoryRepository

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

    // Define playback states
    companion object {
        const val STATE_PLAYING = 3
        const val STATE_PAUSED = 2
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Playing state changed: $isPlaying")
            _isPlaying.value = isPlaying

            // Start or stop position poller based on playing state
            if (isPlaying) {
                startPositionPoller()
                safelyUpdateNotification()
            } else {
                stopPositionPoller()
                safelyUpdateNotification()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "Playback state changed: $playbackState")
            if (playbackState == Player.STATE_ENDED) {
                // Tambahkan lagu ke riwayat jika sudah selesai diputar
                _currentMusic.value?.let { music ->
                    serviceScope.launch {
                        historyRepository.addToHistory(
                            musicId = music.id,
                            playDuration = music.duration
                        )
                    }
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
                // Perbarui info lagu saat ini
                updateCurrentMusic(it)
                safelyUpdateNotification()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "Player error: ${error.message}", error)
        }
    }

    inner class MusicPlayerBinder : Binder() {
        fun getService(): MusicPlayerService = this@MusicPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        try {
            // Inisialisasi CoverArtManager
            coverArtManager = CoverArtManager(this)

            // Inisialisasi ExoPlayer
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
        var albumArt: Bitmap? = null
        try {
            if (music.albumArtPath != null) {
                serviceScope.launch(Dispatchers.IO) {
                    // Gunakan CoverArtManager untuk memuat album art
                    val bitmap = coverArtManager.loadCoverArtBitmap(music.albumArtPath)
                    if (bitmap != null) {
                        createAndShowNotification(music, bitmap)
                    } else {
                        // Jika gagal memuat, tampilkan notifikasi tanpa album art
                        createAndShowNotification(music, null)
                    }
                }
            } else {
                // Jika tidak ada album art, tampilkan notifikasi tanpa album art
                createAndShowNotification(music, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in updateNotification: ${e.message}", e)
            // Jika terjadi error, tetap tampilkan notifikasi tanpa album art
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

        // Cancel semua coroutines
        serviceScope.cancel()

        super.onDestroy()
    }

    private fun startPositionPoller() {
        // Cancel existing job if any
        stopPositionPoller()

        // Start new poller
        positionPollerJob = serviceScope.launch {
            try {
                while (isActive && isServiceActive && _isPlaying.value) {
                    _playbackPosition.value = player.currentPosition
                    kotlinx.coroutines.delay(1000) // Update setiap 1 detik
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

            // Dapatkan info lagu dari extras
            val extras = mediaItem.mediaMetadata.extras
            extras?.let { bundle ->
                val music = bundle.getMusicFromExtras()
                _currentMusic.value = music
                Log.d(TAG, "Current music updated to: ${music.title}")

                // Tambahkan ke riwayat
                serviceScope.launch {
                    try {
                        historyRepository.addToHistory(musicId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding to history: ${e.message}", e)
                    }
                }

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

            // Set daftar playlist jika ada
            if (playlistSongs.isNotEmpty()) {
                setPlaylist(playlistSongs, music)
            } else {
                // Putar lagu tunggal
                val mediaItem = createMediaItem(music)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

                _currentMusic.value = music
                _isPlaying.value = true

                Log.d(TAG, "Started playing music: ${music.title}")

                // Update notification
                safelyUpdateNotification()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music: ${e.message}", e)
        }
    }

    fun updateCurrentMusicCoverArt(updatedMusic: Music) {
        try {
            // Update current music dengan cover art yang baru
            _currentMusic.value = updatedMusic

            // Perbarui notifikasi dengan cover art baru
            safelyUpdateNotification()

            // Log untuk memastikan perubahan
            Log.d(TAG, "Updated cover art: ${updatedMusic.albumArtPath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating current music cover art: ${e.message}", e)
        }
    }

    fun setPlaylist(songs: List<Music>, currentMusic: Music? = null) {
        try {
            Log.d(TAG, "Setting playlist with ${songs.size} songs")
            _playlist.value = songs

            // Reset player dan tambahkan semua lagu ke playlist
            player.clearMediaItems()

            val mediaItems = songs.map { createMediaItem(it) }
            player.addMediaItems(mediaItems)
            player.prepare()

            // Jika ada lagu tertentu yang ingin diputar, cari posisinya
            if (currentMusic != null) {
                val index = songs.indexOfFirst { it.id == currentMusic.id }
                if (index != -1) {
                    player.seekTo(index, 0)
                    Log.d(TAG, "Seeking to song at position $index: ${currentMusic.title}")
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
                safelyUpdateNotification()
            } else {
                Log.d(TAG, "No next item available")
            }
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
                Log.d(TAG, "Restarting current track because position > 3s")
            } else if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
                Log.d(TAG, "Skipped to previous media item. Current index: ${player.currentMediaItemIndex}")
                player.playWhenReady = true  // Ensure it plays after skipping
            } else {
                // Jika tidak ada lagu sebelumnya, kembali ke awal lagu saat ini
                player.seekTo(0)
                Log.d(TAG, "No previous item available, restarting current track")
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