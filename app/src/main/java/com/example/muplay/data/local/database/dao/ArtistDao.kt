package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Music
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtist(artist: Artist)

    @Update
    suspend fun updateArtist(artist: Artist)

    @Query("SELECT * FROM artist ORDER BY name ASC")
    fun getAllArtists(): Flow<List<Artist>>

    @Query("SELECT * FROM artist WHERE name = :artistName")
    fun getArtistByName(artistName: String): Flow<Artist?>

    @Query("SELECT * FROM music WHERE artist = :artistName ORDER BY album ASC, title ASC")
    fun getMusicByArtist(artistName: String): Flow<List<Music>>

    @Query("SELECT COUNT(*) FROM music WHERE artist = :artistName")
    fun getSongCountForArtist(artistName: String): Flow<Int>

    @Query("UPDATE artist SET coverArtPath = :coverArtPath, lastUpdated = :timestamp WHERE name = :artistName")
    suspend fun updateArtistCover(artistName: String, coverArtPath: String?, timestamp: Long = System.currentTimeMillis())
}