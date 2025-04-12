package com.example.muplay.data.repository

import android.util.Log
import com.example.muplay.data.local.database.dao.HistoryDao
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.model.PlayHistory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    private val TAG = "HistoryRepository"

    suspend fun addToHistory(musicId: Long, playDuration: Long? = null) {
        try {
            // Cek apakah lagu ini baru saja diputar (misalnya dalam 10 detik terakhir)
            // Ini untuk menghindari duplikasi entri yang terlalu dekat
            val recentHistory = historyDao.getRecentHistoryForMusic(
                musicId,
                System.currentTimeMillis() - 10000 // 10 detik yang lalu
            )

            if (recentHistory.isEmpty()) {
                // Jika tidak ada entri baru-baru ini, tambahkan yang baru
                val history = PlayHistory(
                    musicId = musicId,
                    playedAt = System.currentTimeMillis(),
                    playDuration = playDuration
                )
                historyDao.insertHistory(history)
                Log.d(TAG, "Added new history for music ID: $musicId")
            } else {
                // Jika lagu ini baru saja diputar, update entri terakhir saja
                val lastEntry = recentHistory.maxByOrNull { it.playedAt }
                lastEntry?.let {
                    historyDao.updateHistoryEntry(
                        it.id,
                        System.currentTimeMillis(),
                        playDuration ?: it.playDuration
                    )
                    Log.d(TAG, "Updated recent history for music ID: $musicId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to history: ${e.message}", e)
        }
    }

    suspend fun clearHistory() {
        try {
            historyDao.clearAllHistory()
            Log.d(TAG, "Cleared all history")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history: ${e.message}", e)
        }
    }

    suspend fun deleteHistoryEntry(historyId: Long) {
        try {
            historyDao.deleteHistoryById(historyId)
            Log.d(TAG, "Deleted history entry: $historyId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history entry: ${e.message}", e)
        }
    }

    suspend fun deleteHistoryForMusic(musicId: Long) {
        try {
            historyDao.deleteHistoryForMusic(musicId)
            Log.d(TAG, "Deleted all history for music ID: $musicId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting history for music: ${e.message}", e)
        }
    }

    suspend fun removeDuplicateHistory() {
        try {
            val duplicatesRemoved = historyDao.removeDuplicateEntries()
            Log.d(TAG, "Removed $duplicatesRemoved duplicate history entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing duplicate history: ${e.message}", e)
        }
    }

    fun getAllHistory(): Flow<List<MusicWithHistory>> =
        historyDao.getHistoryWithMusic()

    fun getRecentlyPlayed(limit: Int = 10): Flow<List<MusicWithHistory>> =
        historyDao.getRecentlyPlayedWithMusic(limit)

    fun getMostPlayed(limit: Int = 10): Flow<List<MusicWithHistory>> =
        historyDao.getMostPlayedWithMusic(limit)
}