package com.example.muplay.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.repository.HistoryRepository
import com.example.muplay.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    // State untuk query pencarian
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // State untuk filter artis yang dipilih
    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist = _selectedArtist.asStateFlow()

    // State untuk filter genre yang dipilih
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre = _selectedGenre.asStateFlow()

    // Filtered songs berdasarkan pencarian dan filter - FIXED: gunakan combine untuk lebih efisien
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredSongs: StateFlow<List<Music>> = combine(
        _searchQuery,
        _selectedArtist,
        _selectedGenre
    ) { query, artist, genre ->
        Triple(query, artist, genre)
    }.flatMapLatest { (query, artist, genre) ->
        when {
            // Prioritaskan pencarian
            query.isNotEmpty() -> musicRepository.searchMusic(query)
            // Filter berdasarkan artis
            artist != null -> musicRepository.getMusicByArtist(artist)
            // Filter berdasarkan genre
            genre != null -> musicRepository.getMusicByGenre(genre)
            // Tampilkan semua jika tidak ada filter
            else -> musicRepository.getAllMusic()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Daftar semua artis untuk filter
    val artists = musicRepository.getDistinctArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daftar semua genre untuk filter
    val genres = musicRepository.getDistinctGenres()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lagu yang terakhir diputar
    val recentlyPlayed = historyRepository.getRecentlyPlayed(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Lagu yang paling sering diputar
    val mostPlayed = historyRepository.getMostPlayed(10)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Total lagu di perangkat
    val totalSongs = musicRepository.getAllMusic()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Update query pencarian
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Pilih artis untuk filter
    fun selectArtist(artist: String?) {
        _selectedArtist.value = artist
        // Reset filter genre jika artis dipilih
        if (artist != null) {
            _selectedGenre.value = null
        }
    }

    // Pilih genre untuk filter
    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
        // Reset filter artis jika genre dipilih
        if (genre != null) {
            _selectedArtist.value = null
        }
    }

    // Reset semua filter
    fun resetFilters() {
        _searchQuery.value = ""
        _selectedArtist.value = null
        _selectedGenre.value = null
    }

    init {
        viewModelScope.launch {
            try {
                // Refresh library musik saat ViewModel dibuat
                musicRepository.refreshMusicLibrary()
            } catch (e: Exception) {
                // Tangani error jika terjadi masalah saat memuat library musik
                // Misalnya: catat ke log, tampilkan pesan error, dsb.
            }
        }
    }

    // Konversi MusicWithHistory ke Music
    fun getMusicFromHistory(musicWithHistory: MusicWithHistory): Music {
        return musicWithHistory.music
    }
}