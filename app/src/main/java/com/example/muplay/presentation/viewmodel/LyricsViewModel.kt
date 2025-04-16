package com.example.muplay.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.LrcContent
import com.example.muplay.data.model.LyricLine
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.LyricRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LyricsViewModel @Inject constructor(
    private val lyricRepository: LyricRepository
) : ViewModel() {
    private val TAG = "LyricsViewModel"

    // Current music track
    private val _currentMusic = MutableStateFlow<Music?>(null)
    val currentMusic: StateFlow<Music?> = _currentMusic.asStateFlow()

    // LRC content
    private val _lrcContent = MutableStateFlow<LrcContent>(LrcContent())
    val lrcContent: StateFlow<LrcContent> = _lrcContent.asStateFlow()

    // Synchronized lyrics with current line highlighted
    private val _synchronizedLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val synchronizedLyrics: StateFlow<List<LyricLine>> = _synchronizedLyrics.asStateFlow()

    // Current playback position
    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    // Current lyric line
    private val _currentLyricLine = MutableStateFlow<LyricLine?>(null)
    val currentLyricLine: StateFlow<LyricLine?> = _currentLyricLine.asStateFlow()

    // Has lyrics
    private val _hasLyrics = MutableStateFlow(false)
    val hasLyrics: StateFlow<Boolean> = _hasLyrics.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error message
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Manual scroll active
    private val _manualScrollActive = MutableStateFlow(false)
    val manualScrollActive: StateFlow<Boolean> = _manualScrollActive.asStateFlow()

    // Auto-scroll resumption job
    private var autoScrollResumptionJob: Job? = null

    /**
     * Initialize with a music track
     */
    fun init(music: Music) {
        if (_currentMusic.value?.id == music.id) return

        viewModelScope.launch {
            _currentMusic.value = music
            loadLyrics(music.id)
        }
    }

    /**
     * Load lyrics for a music track
     */
    private suspend fun loadLyrics(musicId: Long) {
        _isLoading.value = true
        try {
            val hasLyrics = lyricRepository.hasLyrics(musicId)
            _hasLyrics.value = hasLyrics

            if (hasLyrics) {
                val content = lyricRepository.loadLrcContent(musicId)
                _lrcContent.value = content
                updateSynchronizedLyrics(_playbackPosition.value)
            } else {
                _lrcContent.value = LrcContent()
                _synchronizedLyrics.value = emptyList()
                _currentLyricLine.value = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading lyrics: ${e.message}", e)
            _errorMessage.value = "Failed to load lyrics: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Add lyrics from a URI
     */
    fun addLyricsFromUri(uri: Uri) {
        val musicId = _currentMusic.value?.id ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = lyricRepository.addLyricsFromUri(musicId, uri)
                if (success) {
                    loadLyrics(musicId)
                } else {
                    _errorMessage.value = "Failed to add lyrics"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding lyrics: ${e.message}", e)
                _errorMessage.value = "Error adding lyrics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete lyrics for the current track
     */
    fun deleteLyrics() {
        val musicId = _currentMusic.value?.id ?: return

        viewModelScope.launch {
            try {
                lyricRepository.deleteLyrics(musicId)
                _hasLyrics.value = false
                _lrcContent.value = LrcContent()
                _synchronizedLyrics.value = emptyList()
                _currentLyricLine.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting lyrics: ${e.message}", e)
                _errorMessage.value = "Error deleting lyrics: ${e.message}"
            }
        }
    }

    /**
     * Update position, which updates the current lyric line
     */
    fun updatePosition(position: Long) {
        if (_manualScrollActive.value) return

        _playbackPosition.value = position
        viewModelScope.launch {
            updateSynchronizedLyrics(position)
        }
    }

    /**
     * Update synchronized lyrics based on playback position
     */
    private suspend fun updateSynchronizedLyrics(position: Long) {
        val content = _lrcContent.value
        if (content.lines.isEmpty()) return

        _synchronizedLyrics.value = lyricRepository.getSynchronizedLyrics(content, position)
        _currentLyricLine.value = lyricRepository.getCurrentLyricLine(content, position)
    }

    /**
     * Called when user starts manual scrolling
     */
    fun onManualScrollStarted() {
        _manualScrollActive.value = true
        autoScrollResumptionJob?.cancel()
    }

    /**
     * Called when user stops manual scrolling
     */
    fun onManualScrollEnded() {
        autoScrollResumptionJob?.cancel()
        autoScrollResumptionJob = viewModelScope.launch {
            delay(5000) // Wait 5 seconds before resuming auto-scroll
            _manualScrollActive.value = false
            updateSynchronizedLyrics(_playbackPosition.value)
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        autoScrollResumptionJob?.cancel()
    }
} a