package com.example.muplay.data.repository

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
    suspend fun addToHistory(musicId: Long, playDuration: Long? = null) {
        val history = PlayHistory(
            musicId = musicId,
            playedAt = System.currentTimeMillis(),
            playDuration = playDuration
        )
        historyDao.insertHistory(history)
    }

    suspend fun clearHistory() {
        historyDao.clearAllHistory()
    }

    suspend fun deleteHistoryEntry(historyId: Long) {
        historyDao.deleteHistoryById(historyId)
    }

    suspend fun deleteHistoryForMusic(musicId: Long) {
        historyDao.deleteHistoryForMusic(musicId)
    }

    fun getAllHistory(): Flow<List<MusicWithHistory>> =
        historyDao.getHistoryWithMusic()

    fun getRecentlyPlayed(limit: Int = 10): Flow<List<MusicWithHistory>> =
        historyDao.getRecentlyPlayedWithMusic(limit)

    fun getMostPlayed(limit: Int = 10): Flow<List<MusicWithHistory>> =
        historyDao.getMostPlayedWithMusic(limit)
}