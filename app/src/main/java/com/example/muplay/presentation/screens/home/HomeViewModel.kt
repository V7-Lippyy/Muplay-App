package com.example.muplay.presentation.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.repository.HistoryRepository
import com.example.muplay.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val historyRepository: HistoryRepository
) : ViewModel() {
    private val TAG = "HomeViewModel"

    // State for search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // State for selected artist filter
    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist = _selectedArtist.asStateFlow()

    // State for selected genre filter
    private val _selectedGenre = MutableStateFlow<String?>(null)
    val selectedGenre = _selectedGenre.asStateFlow()

    // Force refresh trigger
    private val _forceRefresh = MutableStateFlow(0)

    // Filtered songs based on search and filters - using combine for efficiency
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredSongs: StateFlow<List<Music>> = combine(
        _searchQuery,
        _selectedArtist,
        _selectedGenre
    ) { query, artist, genre ->
        Triple(query, artist, genre)
    }.flatMapLatest { (query, artist, genre) ->
        when {
            // Prioritize search
            query.isNotEmpty() -> musicRepository.searchMusic(query)
            // Filter by artist
            artist != null -> musicRepository.getMusicByArtist(artist)
            // Filter by genre
            genre != null -> musicRepository.getMusicByGenre(genre)
            // Show all if no filters
            else -> musicRepository.getAllMusic()
        }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in filteredSongs flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(30000),
            initialValue = emptyList()
        )

    // List of all artists for filter
    val artists = musicRepository.getDistinctArtists()
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in artists flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(30000),
            initialValue = emptyList()
        )

    // List of all genres for filter
    val genres = musicRepository.getDistinctGenres()
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in genres flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(30000),
            initialValue = emptyList()
        )

    // CRITICAL: Recently played songs - use combine with _forceRefresh to ensure updates
    // Use Eagerly so data is always available
    val recentlyPlayed = combine(
        historyRepository.getRecentSixPlayed(),
        _forceRefresh
    ) { history, _ ->
        Log.d(TAG, "Recently played refreshed, found ${history.size} items")
        history
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in recentlyPlayed flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Most played songs
    val mostPlayed = historyRepository.getMostPlayed(6)
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in mostPlayed flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(30000),
            initialValue = emptyList()
        )

    // Total songs on device
    val totalSongs = musicRepository.getAllMusic()
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
        .catch { e ->
            Log.e(TAG, "Error in totalSongs flow: ${e.message}", e)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(30000),
            initialValue = emptyList()
        )

    // Update search query
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Select artist for filter
    fun selectArtist(artist: String?) {
        _selectedArtist.value = artist
        // Reset genre filter if artist is selected
        if (artist != null) {
            _selectedGenre.value = null
        }
    }

    // Select genre for filter
    fun selectGenre(genre: String?) {
        _selectedGenre.value = genre
        // Reset artist filter if genre is selected
        if (genre != null) {
            _selectedArtist.value = null
        }
    }

    // Reset all filters
    fun resetFilters() {
        _searchQuery.value = ""
        _selectedArtist.value = null
        _selectedGenre.value = null
    }

    // Force refresh the recently played list
    fun refreshRecentlyPlayed() {
        Log.d(TAG, "Forcing refresh of recently played")
        _forceRefresh.value = _forceRefresh.value + 1
    }

    init {
        // Immediately refresh history data
        refreshRecentlyPlayed()

        viewModelScope.launch {
            try {
                // Refresh music library when ViewModel is created
                musicRepository.refreshMusicLibrary()

                // Refresh again after music library loaded
                refreshRecentlyPlayed()
            } catch (e: Exception) {
                Log.e(TAG, "Error in init: ${e.message}", e)
            }
        }
    }

    // Convert MusicWithHistory to Music
    fun getMusicFromHistory(musicWithHistory: MusicWithHistory): Music {
        return musicWithHistory.music
    }
}