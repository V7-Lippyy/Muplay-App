package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.model.PlayHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PlayHistory): Long

    @Query("DELETE FROM play_history WHERE id = :historyId")
    suspend fun deleteHistoryById(historyId: Long)

    @Query("DELETE FROM play_history WHERE musicId = :musicId")
    suspend fun deleteHistoryForMusic(musicId: Long)

    @Query("DELETE FROM play_history")
    suspend fun clearAllHistory()

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN play_history h ON m.id = h.musicId ORDER BY h.playedAt DESC")
    fun getHistoryWithMusic(): Flow<List<MusicWithHistory>>

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN play_history h ON m.id = h.musicId ORDER BY h.playedAt DESC LIMIT :limit")
    fun getRecentlyPlayedWithMusic(limit: Int): Flow<List<MusicWithHistory>>

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN (SELECT musicId, COUNT(*) as playCount, MAX(playedAt) as lastPlayed FROM play_history GROUP BY musicId ORDER BY playCount DESC LIMIT :limit) as popular ON m.id = popular.musicId JOIN play_history h ON h.musicId = m.id AND h.playedAt = popular.lastPlayed")
    fun getMostPlayedWithMusic(limit: Int): Flow<List<MusicWithHistory>>
}