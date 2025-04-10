package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.muplay.data.model.Music
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(music: List<Music>)

    @Update
    suspend fun updateMusic(music: Music)

    @Query("DELETE FROM music")
    suspend fun deleteAll()

    @Query("SELECT * FROM music ORDER BY title ASC")
    fun getAllMusic(): Flow<List<Music>>

    @Query("SELECT * FROM music WHERE id = :id")
    fun getMusicById(id: Long): Flow<Music?>

    @Query("SELECT * FROM music WHERE title LIKE :query OR artist LIKE :query OR album LIKE :query")
    fun searchMusic(query: String): Flow<List<Music>>

    @Query("SELECT * FROM music WHERE artist = :artist ORDER BY title ASC")
    fun getMusicByArtist(artist: String): Flow<List<Music>>

    @Query("SELECT * FROM music WHERE genre = :genre ORDER BY title ASC")
    fun getMusicByGenre(genre: String): Flow<List<Music>>

    @Query("SELECT DISTINCT artist FROM music ORDER BY artist ASC")
    fun getDistinctArtists(): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM music WHERE genre IS NOT NULL ORDER BY genre ASC")
    fun getDistinctGenres(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM music")
    fun getMusicCount(): Flow<Int>
}