package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef
import com.example.muplay.data.model.PlaylistWithMusic
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    // Playlist operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("DELETE FROM playlist WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("SELECT * FROM playlist ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    fun getPlaylistById(playlistId: Long): Flow<Playlist?>

    // Playlist with music operations
    @Transaction
    @Query("SELECT * FROM playlist WHERE id = :playlistId")
    fun getPlaylistWithMusic(playlistId: Long): Flow<PlaylistWithMusic?>

    @Transaction
    @Query("SELECT * FROM playlist ORDER BY updatedAt DESC")
    fun getAllPlaylistsWithMusic(): Flow<List<PlaylistWithMusic>>

    // PlaylistMusicCrossRef operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistMusicCrossRef(crossRef: PlaylistMusicCrossRef)

    @Query("DELETE FROM playlist_music_cross_ref WHERE playlistId = :playlistId AND musicId = :musicId")
    suspend fun deletePlaylistMusicCrossRef(playlistId: Long, musicId: Long)

    @Query("UPDATE playlist_music_cross_ref SET position = :newPosition WHERE playlistId = :playlistId AND musicId = :musicId")
    suspend fun updateMusicPosition(playlistId: Long, musicId: Long, newPosition: Int)

    @Query("SELECT MAX(position) FROM playlist_music_cross_ref WHERE playlistId = :playlistId")
    suspend fun getMaxPositionInPlaylist(playlistId: Long): Int?

    @Query("SELECT * FROM playlist_music_cross_ref WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getPlaylistMusicCrossRefs(playlistId: Long): List<PlaylistMusicCrossRef>
}