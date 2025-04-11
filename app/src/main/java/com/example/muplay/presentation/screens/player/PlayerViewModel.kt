package com.example.muplay.presentation.screens.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.MusicRepository
import com.example.muplay.service.MusicPlayerService
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private var musicPlayerService: MusicPlayerService? = null
    private var bound = false
    private var pendingMusicId: Long? = null
    private val coverArtManager = CoverArtManager(context)

    // Placeholder state flows jika service belum terhubung
    private val _currentMusic = MutableStateFlow<Music?>(null)
    private val _isPlaying = MutableStateFlow(false)
    private val _playbackPosition = MutableStateFlow(0L)
    private val _shuffleMode = MutableStateFlow(false)
    private val _repeatMode = MutableStateFlow(0)
    private val _volume = MutableStateFlow(1.0f)
    val volume = _volume.asStateFlow()

    // UI states yang disinkronkan dengan service
    val currentMusic: StateFlow<Music?> get() = musicPlayerService?.currentMusic ?: _currentMusic
    val isPlaying: StateFlow<Boolean> get() = musicPlayerService?.isPlaying ?: _isPlaying
    val playbackPosition: StateFlow<Long> get() = musicPlayerService?.playbackPosition ?: _playbackPosition
    val shuffleMode: StateFlow<Boolean> get() = musicPlayerService?.shuffleMode ?: _shuffleMode
    val repeatMode: StateFlow<Int> get() = musicPlayerService?.repeatMode ?: _repeatMode

    // Service connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("PlayerViewModel", "Service connected")
            val binder = service as MusicPlayerService.MusicPlayerBinder
            musicPlayerService = binder.getService()
            bound = true

            // Sync state dari service ke ViewModel saat koneksi pertama kali
            syncServiceState()

            // Play any pending music
            pendingMusicId?.let {
                Log.d("PlayerViewModel", "Playing pending music: $it")
                playMusicById(it)
                pendingMusicId = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("PlayerViewModel", "Service disconnected")
            musicPlayerService = null
            bound = false
        }
    }

    init {
        // Bind ke music player service dan start service juga
        Log.d("PlayerViewModel", "Initializing PlayerViewModel")
        startAndBindMusicService()
    }

    // Update music metadata
    fun updateMusicMetadata(musicId: Long, title: String, artist: String, album: String) {
        viewModelScope.launch {
            try {
                // Update in repository
                musicRepository.updateMusicMetadata(musicId, title, artist, album)

                // If this is the currently playing music, update the UI state
                if (_currentMusic.value?.id == musicId) {
                    val updatedMusic = _currentMusic.value?.copy(
                        title = title,
                        artist = artist,
                        album = album
                    )
                    updatedMusic?.let {
                        _currentMusic.value = it

                        // Update the service data if bound
                        musicPlayerService?.updateCurrentMusicMetadata(it)
                    }
                }

                Log.d("PlayerViewModel", "Updated metadata for music ID: $musicId")
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error updating metadata: ${e.message}", e)
            }
        }
    }

    // Update music with custom cover art
    fun updateCustomCoverArt(musicId: Long, uri: Uri) {
        viewModelScope.launch {
            try {
                // Save the image to permanent storage using CoverArtManager
                val coverArtPath = coverArtManager.saveCoverArtFromUri(uri, musicId)

                if (coverArtPath != null) {
                    Log.d("PlayerViewModel", "Saving cover art path: $coverArtPath for music ID: $musicId")

                    // Update the music in the repository with persistent storage
                    musicRepository.updateMusicCoverArt(musicId, coverArtPath)

                    // Refresh the current music object if it's the one being played
                    if (_currentMusic.value?.id == musicId) {
                        val updatedMusic = musicRepository.getMusicById(musicId).first()
                        updatedMusic?.let {
                            // Verify cover art exists after saving
                            val exists = coverArtManager.coverArtExists(coverArtPath)
                            Log.d("PlayerViewModel", "Cover art exists after saving: $exists")

                            // Update local state
                            _currentMusic.value = it

                            // Update service if bound
                            musicPlayerService?.updateCurrentMusicCoverArt(it)
                        }
                    }
                } else {
                    Log.e("PlayerViewModel", "Failed to save cover art for music ID: $musicId")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error updating cover art: ${e.message}", e)
            }
        }
    }

    private fun startAndBindMusicService() {
        Log.d("PlayerViewModel", "Starting and binding music service")
        Intent(context, MusicPlayerService::class.java).also { intent ->
            // Start service first to keep it running
            context.startService(intent)
            // Then bind to it
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    // Sinkronisasi state dari service saat pertama kali terhubung
    private fun syncServiceState() {
        musicPlayerService?.let { service ->
            Log.d("PlayerViewModel", "Syncing state from service")
            viewModelScope.launch {
                service.currentMusic.collect { music ->
                    _currentMusic.value = music

                    // Verify cover art exists for the current music
                    music?.let {
                        val exists = coverArtManager.coverArtExists(music.albumArtPath)
                        Log.d("PlayerViewModel", "Cover art exists for current music: $exists, path: ${music.albumArtPath}")
                    }
                }
            }

            viewModelScope.launch {
                service.isPlaying.collect { playing ->
                    _isPlaying.value = playing
                }
            }

            viewModelScope.launch {
                service.playbackPosition.collect { position ->
                    _playbackPosition.value = position
                }
            }

            viewModelScope.launch {
                service.shuffleMode.collect { shuffle ->
                    _shuffleMode.value = shuffle
                }
            }

            viewModelScope.launch {
                service.repeatMode.collect { repeat ->
                    _repeatMode.value = repeat
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("PlayerViewModel", "ViewModel cleared, unbinding service")
        if (bound) {
            context.unbindService(serviceConnection)
            bound = false
        }
    }

    // Function untuk memutar lagu berdasarkan ID
    fun playMusic(musicId: Long) {
        Log.d("PlayerViewModel", "playMusic called with ID: $musicId")

        if (!bound || musicPlayerService == null) {
            Log.d("PlayerViewModel", "Service not bound yet, setting pending music ID")
            pendingMusicId = musicId
            startAndBindMusicService()
            return
        }

        playMusicById(musicId)
    }

    private fun playMusicById(musicId: Long) {
        viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "Fetching music with ID: $musicId")

                // Get all music for playlist
                val allMusic = musicRepository.getAllMusic().first()

                // Get music by ID, using first() to get a single value instead of collecting indefinitely
                val music = musicRepository.getMusicById(musicId).first()

                if (music != null) {
                    Log.d("PlayerViewModel", "Found music: ${music.title}, playing it now")

                    // Verify cover art exists
                    val exists = coverArtManager.coverArtExists(music.albumArtPath)
                    Log.d("PlayerViewModel", "Cover art exists before playing: $exists, path: ${music.albumArtPath}")

                    // Check music file exists
                    if (music.uri.isNotEmpty()) {
                        // Play the music with the complete playlist
                        musicPlayerService?.playMusic(music, allMusic)
                    } else {
                        Log.e("PlayerViewModel", "Music URI is empty for ID: $musicId")
                    }
                } else {
                    Log.e("PlayerViewModel", "Music with ID $musicId not found")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error playing music with ID $musicId: ${e.message}", e)
            }
        }
    }

    // Control functions with additional error handling
    fun togglePlayPause() {
        try {
            musicPlayerService?.togglePlayPause()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error toggling play/pause: ${e.message}", e)
        }
    }

    fun skipToNext() {
        try {
            musicPlayerService?.skipToNext()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error skipping to next: ${e.message}", e)
        }
    }

    fun skipToPrevious() {
        try {
            musicPlayerService?.skipToPrevious()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error skipping to previous: ${e.message}", e)
        }
    }

    fun seekTo(position: Long) {
        try {
            musicPlayerService?.seekTo(position)
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error seeking to position: ${e.message}", e)
        }
    }

    fun toggleShuffleMode() {
        try {
            musicPlayerService?.toggleShuffleMode()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error toggling shuffle mode: ${e.message}", e)
        }
    }

    fun toggleRepeatMode() {
        try {
            musicPlayerService?.toggleRepeatMode()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error toggling repeat mode: ${e.message}", e)
        }
    }

    fun setVolume(volume: Float) {
        try {
            _volume.value = volume
            musicPlayerService?.setVolume(volume)
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error setting volume: ${e.message}", e)
        }
    }
}