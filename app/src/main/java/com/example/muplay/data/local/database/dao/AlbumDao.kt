package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Music
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlbum(album: Album)

    @Update
    suspend fun updateAlbum(album: Album)

    @Query("SELECT * FROM album ORDER BY name ASC")
    fun getAllAlbums(): Flow<List<Album>>

    @Query("SELECT * FROM album WHERE name = :albumName")
    fun getAlbumByName(albumName: String): Flow<Album?>

    @Query("SELECT * FROM music WHERE album = :albumName ORDER BY trackNumber ASC, title ASC")
    fun getMusicByAlbum(albumName: String): Flow<List<Music>>

    @Query("SELECT COUNT(*) FROM music WHERE album = :albumName")
    fun getSongCountForAlbum(albumName: String): Flow<Int>

    @Query("UPDATE album SET coverArtPath = :coverArtPath, lastUpdated = :timestamp WHERE name = :albumName")
    suspend fun updateAlbumCover(albumName: String, coverArtPath: String?, timestamp: Long = System.currentTimeMillis())
}