package com.example.muplay.data.repository

import com.example.muplay.data.local.database.dao.PlaylistDao
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef
import com.example.muplay.data.model.PlaylistWithMusic
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
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
}