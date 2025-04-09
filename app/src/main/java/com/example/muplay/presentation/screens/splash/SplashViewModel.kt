package com.example.muplay.presentation.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val musicRepository: MusicRepository
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
        // Inisialisasi repository dan data yang dibutuhkan saat pertama kali
        musicRepository.refreshMusicLibrary()
    }
}