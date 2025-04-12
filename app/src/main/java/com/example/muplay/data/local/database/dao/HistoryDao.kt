package com.example.muplay.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Query("UPDATE play_history SET playedAt = :playedAt, playDuration = :playDuration WHERE id = :historyId")
    suspend fun updateHistoryEntry(historyId: Long, playedAt: Long, playDuration: Long?)

    @Query("SELECT * FROM play_history WHERE musicId = :musicId AND playedAt > :minTimestamp")
    suspend fun getRecentHistoryForMusic(musicId: Long, minTimestamp: Long): List<PlayHistory>

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN play_history h ON m.id = h.musicId ORDER BY h.playedAt DESC")
    fun getHistoryWithMusic(): Flow<List<MusicWithHistory>>

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN play_history h ON m.id = h.musicId ORDER BY h.playedAt DESC LIMIT :limit")
    fun getRecentlyPlayedWithMusic(limit: Int): Flow<List<MusicWithHistory>>

    @Transaction
    @Query("SELECT m.*, h.id as historyId, h.playedAt, h.playDuration FROM music m JOIN (SELECT musicId, COUNT(*) as playCount, MAX(playedAt) as lastPlayed FROM play_history GROUP BY musicId ORDER BY playCount DESC LIMIT :limit) as popular ON m.id = popular.musicId JOIN play_history h ON h.musicId = m.id AND h.playedAt = popular.lastPlayed")
    fun getMostPlayedWithMusic(limit: Int): Flow<List<MusicWithHistory>>

    /**
     * Menghapus entri duplikat, menyimpan hanya entri terbaru untuk setiap
     * kombinasi musicId yang sama yang dimainkan pada hari yang sama
     * @return jumlah entri yang dihapus
     */
    @Transaction
    suspend fun removeDuplicateEntries(): Int {
        // 1. Dapatkan semua entri yang merupakan duplikat (bukan yang terbaru dari musicId sama pada hari yang sama)
        val duplicates = getDuplicateEntries()

        // 2. Hapus semua entri yang ditemukan
        for (id in duplicates) {
            deleteHistoryById(id)
        }

        return duplicates.size
    }

    /**
     * Mendapatkan ID dari entri duplikat yang bukan yang terbaru
     * untuk setiap kombinasi musicId yang sama
     */
    @Query("""
        SELECT ph1.id
        FROM play_history ph1
        WHERE EXISTS (
            SELECT 1
            FROM play_history ph2
            WHERE ph1.musicId = ph2.musicId
            AND ph2.playedAt > ph1.playedAt
            AND date(ph1.playedAt/1000, 'unixepoch') = date(ph2.playedAt/1000, 'unixepoch')
        )
    """)
    suspend fun getDuplicateEntries(): List<Long>
}