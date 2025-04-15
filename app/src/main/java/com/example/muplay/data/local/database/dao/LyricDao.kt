package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.muplay.data.model.Lyric
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLyric(lyric: Lyric): Long

    @Update
    suspend fun updateLyric(lyric: Lyric)

    @Query("SELECT * FROM lyric WHERE musicId = :musicId")
    fun getLyricForMusic(musicId: Long): Flow<Lyric?>

    @Query("SELECT EXISTS(SELECT 1 FROM lyric WHERE musicId = :musicId)")
    suspend fun hasLyrics(musicId: Long): Boolean

    @Query("DELETE FROM lyric WHERE musicId = :musicId")
    suspend fun deleteLyric(musicId: Long)

    @Query("UPDATE lyric SET lrcPath = :lrcPath, lastUpdated = :timestamp WHERE musicId = :musicId")
    suspend fun updateLrcPath(musicId: Long, lrcPath: String?, timestamp: Long = System.currentTimeMillis())
}