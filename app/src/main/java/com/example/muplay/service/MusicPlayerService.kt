package com.example.muplay.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.HistoryRepository
import com.example.muplay.util.Constants
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
import javax.inject.Inject

private const val TAG = "MusicPlayerService"

@AndroidEntryPoint
class MusicPlayerService : Service() {
    // Custom CoroutineScope untuk service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var historyRepository: HistoryRepository

    private val binder = MusicPlayerBinder()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private var positionPollerJob: Job? = null
    private var isServiceActive = true

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

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "Playing state changed: $isPlaying")
            _isPlaying.value = isPlaying

            // Start or stop position poller based on playing state
            if (isPlaying) {
                startPositionPoller()
            } else {
                stopPositionPoller()
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
            // Inisialisasi ExoPlayer
            player = ExoPlayer.Builder(this).build().apply {
                addListener(playerListener)
            }

            // Buat media session
            mediaSession = MediaSession.Builder(this, player).build()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing player: ${e.message}", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service start command received")
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
            mediaSession.release()
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
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing music: ${e.message}", e)
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
        } catch (e: Exception) {
            Log.e(TAG, "Error in togglePlayPause: ${e.message}", e)
        }
    }

    fun skipToNext() {
        try {
            Log.d(TAG, "Skip to next")
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in skipToNext: ${e.message}", e)
        }
    }

    fun skipToPrevious() {
        try {
            Log.d(TAG, "Skip to previous")
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            } else {
                // Jika tidak ada lagu sebelumnya, kembali ke awal lagu saat ini
                player.seekTo(0)
            }
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
        uri = getString(Constants.EXTRA_MUSIC_URI, ""), // Make sure this key exists in Constants.kt
        albumArtPath = getString(Constants.EXTRA_MUSIC_ART_URI)
    )
}