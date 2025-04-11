package com.example.muplay.presentation.screens.collection

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Music
import com.example.muplay.data.repository.ArtistRepository
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artistRepository: ArtistRepository
) : ViewModel() {

    private val _artistName = MutableStateFlow("")

    private val _artistDetails = MutableStateFlow<Artist?>(null)
    val artistDetails: StateFlow<Artist?> = _artistDetails.asStateFlow()

    private val _artistSongs = MutableStateFlow<List<Music>>(emptyList())
    val artistSongs: StateFlow<List<Music>> = _artistSongs.asStateFlow()

    private val coverArtManager = CoverArtManager(context)

    fun initWithArtistName(artistName: String) {
        if (_artistName.value == artistName) return

        _artistName.value = artistName
        loadArtistDetails()
    }

    private fun loadArtistDetails() {
        viewModelScope.launch {
            try {
                // Load artist details
                artistRepository.getArtistByName(_artistName.value).collectLatest { artist ->
                    _artistDetails.value = artist
                }
            } catch (e: Exception) {
                Log.e("ArtistDetailViewModel", "Error loading artist details", e)
            }
        }

        viewModelScope.launch {
            try {
                // Load songs by the artist
                artistRepository.getMusicByArtist(_artistName.value).collectLatest { songs ->
                    _artistSongs.value = songs
                }
            } catch (e: Exception) {
                Log.e("ArtistDetailViewModel", "Error loading artist songs", e)
            }
        }
    }

    suspend fun updateArtistCover(uri: Uri) {
        try {
            // Generate a file name based on artist name
            val artistName = _artistName.value
            if (artistName.isBlank()) return

            val fileName = "artist_image_${artistName.replace(" ", "_").lowercase()}.jpg"

            // Save the image using CoverArtManager
            val coverArtPath = coverArtManager.saveCoverArtFromUri(uri, 0, fileName)

            if (coverArtPath != null) {
                // Update the artist cover in repository
                artistRepository.updateArtistCover(artistName, coverArtPath)

                // Update local state (not really needed as we're observing the repository)
                _artistDetails.value = _artistDetails.value?.copy(coverArtPath = coverArtPath)

                Log.d("ArtistDetailViewModel", "Updated artist image: $coverArtPath")
            } else {
                Log.e("ArtistDetailViewModel", "Failed to save artist image")
            }
        } catch (e: Exception) {
            Log.e("ArtistDetailViewModel", "Error updating artist image", e)
        }
    }
}