package com.example.muplay.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.repository.AlbumRepository
import com.example.muplay.data.repository.ArtistRepository
import com.example.muplay.data.repository.MusicRepository
import com.example.muplay.data.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
    private val playlistRepository: PlaylistRepository
) : ViewModel() {

    // Tema gelap atau terang
    val darkTheme = musicRepository.getDarkThemePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    init {
        viewModelScope.launch {
            // Inisialisasi data aplikasi
            initializeAppData()
        }
    }

    private suspend fun initializeAppData() {
        try {
            // Refresh music library
            musicRepository.refreshMusicLibrary()

            // Ambil semua musik yang sudah di-refresh
            val allMusic = musicRepository.getAllMusic().first()

            // Pastikan album dan artist juga di-refresh
            if (allMusic.isNotEmpty()) {
                albumRepository.refreshAlbums(allMusic)
                artistRepository.refreshArtists(allMusic)

                // Bersihkan koleksi kosong
                albumRepository.cleanupEmptyAlbums()
                artistRepository.cleanupEmptyArtists()
                playlistRepository.cleanupEmptyPlaylists()
            }
        } catch (e: Exception) {
            // Log error tapi tetap lanjutkan aplikasi
        }
    }
}