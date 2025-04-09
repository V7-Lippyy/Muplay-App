package com.example.muplay.presentation.screens.playlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistWithMusic
import com.example.muplay.data.repository.MusicRepository
import com.example.muplay.data.repository.PlaylistRepository
import com.example.muplay.service.MusicPlayerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val musicRepository: MusicRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // ID playlist dari navigasi
    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: -1L

    // Data playlist
    val playlistWithMusic = playlistRepository.getPlaylistWithMusic(playlistId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Info playlist
    val playlist: StateFlow<Playlist?> = playlistWithMusic.map { it?.playlist }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Daftar lagu dalam playlist
    val songs: StateFlow<List<Music>> = playlistWithMusic.map { it?.songs ?: emptyList() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Daftar semua lagu untuk ditambahkan ke playlist
    val allMusic = musicRepository.getAllMusic()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Menghapus lagu dari playlist
    fun removeSongFromPlaylist(musicId: Long) {
        viewModelScope.launch {
            playlistRepository.removeMusicFromPlaylist(playlistId, musicId)
        }
    }

    // Menambahkan lagu ke playlist
    fun addSongToPlaylist(musicId: Long) {
        viewModelScope.launch {
            playlistRepository.addMusicToPlaylist(playlistId, musicId)
        }
    }

    // Mengubah nama playlist
    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            playlist.value?.let { currentPlaylist ->
                val updatedPlaylist = currentPlaylist.copy(name = newName)
                playlistRepository.updatePlaylist(updatedPlaylist)
            }
        }
    }

    // Menghapus playlist
    fun deletePlaylist() {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
        }
    }

    // Fungsi untuk mendapatkan lagu yang belum ada di playlist
    fun getSongsNotInPlaylist(): List<Music> {
        val playlistSongIds = songs.value.map { it.id }
        return allMusic.value.filter { it.id !in playlistSongIds }
    }

    // Mengubah posisi lagu dalam playlist
    fun moveSongPosition(musicId: Long, newPosition: Int) {
        viewModelScope.launch {
            playlistRepository.updateMusicPositionInPlaylist(playlistId, musicId, newPosition)
        }
    }

    // Mengatur cover art playlist
    fun setCoverArt(coverArtPath: String?) {
        viewModelScope.launch {
            playlistRepository.setCoverArtForPlaylist(playlistId, coverArtPath)
        }
    }
}