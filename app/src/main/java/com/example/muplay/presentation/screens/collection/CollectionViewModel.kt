package com.example.muplay.presentation.screens.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.repository.AlbumRepository
import com.example.muplay.data.repository.ArtistRepository
import com.example.muplay.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository
) : ViewModel() {

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