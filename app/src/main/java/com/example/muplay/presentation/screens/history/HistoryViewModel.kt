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

    // Status cleanup dan pesan status
    private val _isCleaningUp = MutableStateFlow(false)
    val isCleaningUp = _isCleaningUp.asStateFlow()

    private val _statusMessage = MutableStateFlow("")
    val statusMessage = _statusMessage.asStateFlow()

    // Sort type untuk riwayat
    enum class SortType {
        TIME_DESC,    // Terbaru ke terlama
        TIME_ASC,     // Terlama ke terbaru
        TITLE_ASC,    // Judul A-Z
        TITLE_DESC,   // Judul Z-A
        ARTIST_ASC,   // Artis A-Z
        ARTIST_DESC   // Artis Z-A
    }

    // Filter state
    private val _sortType = MutableStateFlow(SortType.TIME_DESC)
    val sortType = _sortType.asStateFlow()

    // Semua riwayat dari repository
    private val allHistory = historyRepository.getAllHistory()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daftar playlist untuk fungsi 'tambahkan ke playlist'
    val playlists = playlistRepository.getAllPlaylists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // State untuk history yang sudah diurutkan - FIXED: menggunakan combine untuk mengurangi rekomposisi
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
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Saat ViewModel diinisialisasi, bersihkan duplikasi secara otomatis
    init {
        cleanupDuplicateHistory(showMessage = false)
    }

    // Ubah jenis pengurutan
    fun setSortType(type: SortType) {
        _sortType.value = type
    }

    // Hapus entri riwayat tertentu
    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistoryEntry(historyId)
            _statusMessage.value = "Entri riwayat dihapus"
            resetStatusMessageAfterDelay()
        }
    }

    // Tambahkan lagu ke playlist
    fun addToPlaylist(playlistId: Long, musicId: Long) {
        viewModelScope.launch {
            playlistRepository.addMusicToPlaylist(playlistId, musicId)
            _statusMessage.value = "Ditambahkan ke playlist"
            resetStatusMessageAfterDelay()
        }
    }

    // Hapus semua riwayat
    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearHistory()
            _statusMessage.value = "Semua riwayat telah dihapus"
            resetStatusMessageAfterDelay()
        }
    }

    // Bersihkan riwayat duplikat
    fun cleanupDuplicateHistory(showMessage: Boolean = true) {
        viewModelScope.launch {
            try {
                _isCleaningUp.value = true
                historyRepository.removeDuplicateHistory()
                if (showMessage) {
                    _statusMessage.value = "Duplikasi riwayat dibersihkan"
                    resetStatusMessageAfterDelay()
                }
            } catch (e: Exception) {
                if (showMessage) {
                    _statusMessage.value = "Gagal membersihkan duplikasi: ${e.message}"
                    resetStatusMessageAfterDelay()
                }
            } finally {
                _isCleaningUp.value = false
            }
        }
    }

    // Reset status message setelah beberapa detik
    private fun resetStatusMessageAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Tampilkan pesan selama 3 detik
            _statusMessage.value = ""
        }
    }

    // Mendapatkan objek Music dari MusicWithHistory
    fun getMusicFromHistory(musicWithHistory: MusicWithHistory): Music {
        return musicWithHistory.music
    }
}