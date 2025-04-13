package com.example.muplay.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.repository.HistoryRepository
import com.example.muplay.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Status message
    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    // Sort type for history
    enum class SortType {
        TIME_DESC,    // Newest to oldest
        TIME_ASC,     // Oldest to newest
        TITLE_ASC,    // Title A-Z
        TITLE_DESC,   // Title Z-A
        ARTIST_ASC,   // Artist A-Z
        ARTIST_DESC   // Artist Z-A
    }

    // Filter state
    private val _sortType = MutableStateFlow(SortType.TIME_DESC)
    val sortType = _sortType.asStateFlow()

    // All history from repository
    // Changed SharedFlow.Lazily to cache history for 30 minutes (1800000 ms)
    private val allHistory = historyRepository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1800000),
            initialValue = emptyList()
        )

    // Playlists for 'add to playlist' function
    val playlists = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1800000),
            initialValue = emptyList()
        )

    // State for sorted history - Using combine to reduce recomposition
    @OptIn(ExperimentalCoroutinesApi::class)
    val historyList: StateFlow<List<MusicWithHistory>> = combine(
        allHistory,
        _sortType
    ) { history, sortType ->
        when (sortType) {
            SortType.TIME_DESC -> history.sortedByDescending { it.playedAt }
            SortType.TIME_ASC -> history.sortedBy { it.playedAt }
            SortType.TITLE_ASC -> history.sortedBy { it.music.title }
            SortType.TITLE_DESC -> history.sortedByDescending { it.music.title }
            SortType.ARTIST_ASC -> history.sortedBy { it.music.artist }
            SortType.ARTIST_DESC -> history.sortedByDescending { it.music.artist }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(1800000),
        initialValue = emptyList()
    )

    // Change sort type
    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    // Delete specific history entry
    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryEntry(historyId)
            _statusMessage.value = "Entri riwayat dihapus"
            resetStatusMessageAfterDelay()
        }
    }

    // Add song to playlist
    fun addToPlaylist(playlistId: Long, musicId: Long) {
        viewModelScope.launch {
            playlistRepository.addMusicToPlaylist(playlistId, musicId)
            _statusMessage.value = "Ditambahkan ke playlist"
            resetStatusMessageAfterDelay()
        }
    }

    // Clear all history
    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _statusMessage.value = "Semua riwayat telah dihapus"
            resetStatusMessageAfterDelay()
        }
    }

    // Reset status message after a few seconds
    private fun resetStatusMessageAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Show message for 3 seconds
            _statusMessage.value = ""
        }
    }

    // Get Music object from MusicWithHistory
    fun getMusicFromHistory(musicWithHistory: MusicWithHistory): Music {
        return musicWithHistory.music
    }
}