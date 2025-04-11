package com.example.muplay.presentation.screens.collection

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.AlbumRepository
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumRepository: AlbumRepository
) : ViewModel() {

    private val _albumName = MutableStateFlow("")

    private val _albumDetails = MutableStateFlow<Album?>(null)
    val albumDetails: StateFlow<Album?> = _albumDetails.asStateFlow()

    private val _albumSongs = MutableStateFlow<List<Music>>(emptyList())
    val albumSongs: StateFlow<List<Music>> = _albumSongs.asStateFlow()

    private val coverArtManager = CoverArtManager(context)

    fun initWithAlbumName(albumName: String) {
        if (_albumName.value == albumName) return

        _albumName.value = albumName
        loadAlbumDetails()
    }

    private fun loadAlbumDetails() {
        viewModelScope.launch {
            try {
                // Load album details
                albumRepository.getAlbumByName(_albumName.value).collectLatest { album ->
                    _albumDetails.value = album
                }
            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Error loading album details", e)
            }
        }

        viewModelScope.launch {
            try {
                // Load songs in the album
                albumRepository.getMusicByAlbum(_albumName.value).collectLatest { songs ->
                    _albumSongs.value = songs
                }
            } catch (e: Exception) {
                Log.e("AlbumDetailViewModel", "Error loading album songs", e)
            }
        }
    }

    suspend fun updateAlbumCover(uri: Uri) {
        try {
            // Generate a file name based on album name
            val albumName = _albumName.value
            if (albumName.isBlank()) return

            val fileName = "album_cover_${albumName.replace(" ", "_").lowercase()}.jpg"

            // Save the image using CoverArtManager
            val coverArtPath = coverArtManager.saveCoverArtFromUri(uri, 0, fileName)

            if (coverArtPath != null) {
                // Update the album cover in repository
                albumRepository.updateAlbumCover(albumName, coverArtPath)

                // Update local state (not really needed as we're observing the repository)
                _albumDetails.value = _albumDetails.value?.copy(coverArtPath = coverArtPath)

                Log.d("AlbumDetailViewModel", "Updated album cover: $coverArtPath")
            } else {
                Log.e("AlbumDetailViewModel", "Failed to save album cover")
            }
        } catch (e: Exception) {
            Log.e("AlbumDetailViewModel", "Error updating album cover", e)
        }
    }
}