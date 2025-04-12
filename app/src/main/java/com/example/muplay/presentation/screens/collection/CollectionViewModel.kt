package com.example.muplay.presentation.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.repository.AlbumRepository
import com.example.muplay.data.repository.ArtistRepository
import com.example.muplay.data.repository.MusicRepository
import com.example.muplay.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    // Status loading untuk refresh
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Status refresh yang menampilkan pesan jika berhasil
    private val _refreshMessage = MutableStateFlow<String?>(null)
    val refreshMessage: StateFlow<String?> = _refreshMessage.asStateFlow()

    // Playlists data
    val allPlaylists = playlistRepository.getAllPlaylistsWithMusic()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Albums data
    val allAlbums = albumRepository.getAllAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Artists data
    val allArtists = artistRepository.getAllArtists()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Bersihkan koleksi kosong saat pertama kali load
        cleanupEmptyCollections()
    }

    // Force refresh collections
    fun forceRefreshCollections() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true

                // Ambil semua musik
                val allMusic = musicRepository.getAllMusic().first()

                if (allMusic.isNotEmpty()) {
                    // Force refresh collections
                    albumRepository.refreshAlbums(allMusic)
                    artistRepository.refreshArtists(allMusic)

                    // Setelah refresh, bersihkan koleksi yang kosong
                    cleanupEmptyCollections()

                    // Set success message
                    _refreshMessage.value = "Koleksi berhasil diperbarui"
                } else {
                    _refreshMessage.value = "Tidak ada musik ditemukan"
                }
            } catch (e: Exception) {
                _refreshMessage.value = "Gagal memperbarui koleksi: ${e.message}"
            } finally {
                _isRefreshing.value = false

                // Reset message after 3 seconds
                launch {
                    kotlinx.coroutines.delay(3000)
                    _refreshMessage.value = null
                }
            }
        }
    }

    // Membersihkan koleksi yang kosong
    private fun cleanupEmptyCollections() {
        viewModelScope.launch {
            try {
                // Bersihkan album kosong
                albumRepository.cleanupEmptyAlbums()

                // Bersihkan artist kosong
                artistRepository.cleanupEmptyArtists()

                // Bersihkan playlist kosong (terutama yang bernama "Download")
                playlistRepository.cleanupEmptyPlaylists()
            } catch (e: Exception) {
                // Tangani error dengan silent (tidak perlu tampilkan ke user)
            }
        }
    }

    // Create playlist
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            playlistRepository.createPlaylist(name)
        }
    }

    // Update playlist
    fun updatePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            playlistRepository.updatePlaylist(playlist)
        }
    }

    // Update album cover
    fun updateAlbumCover(albumName: String, coverArtPath: String?) {
        viewModelScope.launch {
            albumRepository.updateAlbumCover(albumName, coverArtPath)
        }
    }

    // Update artist cover
    fun updateArtistCover(artistName: String, coverArtPath: String?) {
        viewModelScope.launch {
            artistRepository.updateArtistCover(artistName, coverArtPath)
        }
    }
}