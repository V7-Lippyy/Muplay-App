package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.muplay.data.model.MusicWithPlayCount
import com.example.muplay.data.model.PlayCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayCountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayCount(playCount: PlayCount)

    @Query("UPDATE play_count SET count = count + 1, lastPlayed = :timestamp WHERE musicId = :musicId")
    suspend fun incrementCount(musicId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM play_count WHERE musicId = :musicId")
    fun getPlayCountForMusic(musicId: Long): Flow<PlayCount?>

    @Query("SELECT COUNT(*) FROM play_count WHERE musicId = :musicId")
    suspend fun hasPlayCount(musicId: Long): Int

    @Transaction
    @Query("""
        SELECT m.*, pc.count as playCount, pc.lastPlayed, pc.isFavorite
        FROM music m
        INNER JOIN play_count pc ON m.id = pc.musicId
        WHERE pc.count >= :minCount
        ORDER BY pc.count DESC, pc.lastPlayed DESC
        LIMIT :limit
    """)
    fun getMostPlayedMusic(minCount: Int = 5, limit: Int = 9): Flow<List<MusicWithPlayCount>>

    @Transaction
    @Query("""
        SELECT m.*, pc.count as playCount, pc.lastPlayed, pc.isFavorite
        FROM music m
        INNER JOIN play_count pc ON m.id = pc.musicId
        ORDER BY pc.count DESC, pc.lastPlayed DESC
        LIMIT :limit
    """)
    fun getTopPlayedMusic(limit: Int = 9): Flow<List<MusicWithPlayCount>>

    @Query("UPDATE play_count SET isFavorite = :isFavorite WHERE musicId = :musicId")
    suspend fun setFavorite(musicId: Long, isFavorite: Boolean)

    @Query("DELETE FROM play_count WHERE musicId = :musicId")
    suspend fun deletePlayCount(musicId: Long)

    @Query("DELETE FROM play_count")
    suspend fun deleteAllPlayCounts()
}