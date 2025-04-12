package com.example.muplay.data.repository

import android.util.Log
import com.example.muplay.data.local.database.dao.PlaylistDao
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef
import com.example.muplay.data.model.PlaylistWithMusic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    private val TAG = "PlaylistRepository"

    // Playlist operations
    suspend fun createPlaylist(name: String): Long {
        val timestamp = System.currentTimeMillis()
        val playlist = Playlist(
            name = name,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        return playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: Playlist) {
        val updatedPlaylist = playlist.copy(updatedAt = System.currentTimeMillis())
        playlistDao.updatePlaylist(updatedPlaylist)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    fun getPlaylistById(playlistId: Long): Flow<Playlist?> =
        playlistDao.getPlaylistById(playlistId)

    fun getPlaylistWithMusic(playlistId: Long): Flow<PlaylistWithMusic?> =
        playlistDao.getPlaylistWithMusic(playlistId)

    fun getAllPlaylistsWithMusic(): Flow<List<PlaylistWithMusic>> =
        playlistDao.getAllPlaylistsWithMusic()

    // Music in playlist operations
    suspend fun addMusicToPlaylist(playlistId: Long, musicId: Long) {
        // Hitung posisi berikutnya
        val maxPosition = playlistDao.getMaxPositionInPlaylist(playlistId) ?: -1
        val nextPosition = maxPosition + 1

        val crossRef = PlaylistMusicCrossRef(
            playlistId = playlistId,
            musicId = musicId,
            position = nextPosition
        )

        playlistDao.insertPlaylistMusicCrossRef(crossRef)

        // Update timestamp pada playlist
        playlistDao.getPlaylistById(playlistId).collect { playlist ->
            playlist?.let {
                updatePlaylist(it)
            }
        }
    }

    suspend fun removeMusicFromPlaylist(playlistId: Long, musicId: Long) {
        playlistDao.deletePlaylistMusicCrossRef(playlistId, musicId)

        // Reindeks posisi musik dalam playlist
        reindexPlaylistPositions(playlistId)

        // Update timestamp
        playlistDao.getPlaylistById(playlistId).collect { playlist ->
            playlist?.let {
                updatePlaylist(it)
            }
        }
    }

    suspend fun updateMusicPositionInPlaylist(
        playlistId: Long,
        musicId: Long,
        newPosition: Int
    ) {
        playlistDao.updateMusicPosition(playlistId, musicId, newPosition)

        // Reindeks posisi jika diperlukan
        reindexPlaylistPositions(playlistId)
    }

    private suspend fun reindexPlaylistPositions(playlistId: Long) {
        val crossRefs = playlistDao.getPlaylistMusicCrossRefs(playlistId)

        // Urutkan berdasarkan posisi saat ini
        val sortedCrossRefs = crossRefs.sortedBy { it.position }

        // Perbarui dengan posisi baru
        sortedCrossRefs.forEachIndexed { index, crossRef ->
            if (crossRef.position != index) {
                playlistDao.updateMusicPosition(playlistId, crossRef.musicId, index)
            }
        }
    }

    suspend fun setCoverArtForPlaylist(playlistId: Long, coverArtPath: String?) {
        playlistDao.getPlaylistById(playlistId).collect { playlist ->
            playlist?.let {
                val updatedPlaylist = it.copy(
                    coverArtPath = coverArtPath,
                    updatedAt = System.currentTimeMillis()
                )
                playlistDao.updatePlaylist(updatedPlaylist)
            }
        }
    }

    // Clean up empty playlists
    suspend fun cleanupEmptyPlaylists() {
        try {
            // Get all playlists with their music
            val allPlaylistsWithMusic = withContext(Dispatchers.IO) {
                playlistDao.getAllPlaylistsWithMusic().firstOrNull() ?: emptyList()
            }

            // Find empty playlists
            val emptyPlaylists = allPlaylistsWithMusic.filter { it.songs.isEmpty() }

            // Delete empty playlists if needed
            if (emptyPlaylists.isNotEmpty()) {
                Log.d(TAG, "Found ${emptyPlaylists.size} empty playlists to delete")

                for (playlist in emptyPlaylists) {
                    // Only auto-delete if it's a system-generated playlist (if you want to keep user-created empty playlists)
                    // For example, you could check name == "Download" or have some flag in the playlist model
                    // Here we're just checking if it has "Download" in the name as an example
                    if (playlist.playlist.name.contains("Download", ignoreCase = true)) {
                        deletePlaylist(playlist.playlist.id)
                        Log.d(TAG, "Deleted empty system playlist: ${playlist.playlist.name}")
                    }
                }
            } else {
                Log.d(TAG, "No empty playlists found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up empty playlists", e)
        }
    }
}