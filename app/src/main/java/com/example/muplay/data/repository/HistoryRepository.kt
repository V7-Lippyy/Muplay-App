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
    private val MAX_HISTORY_ENTRIES = 6

    /**
     * Add a song to the history. This method ensures persistence by immediately writing to the database.
     */
    suspend fun addToHistory(musicId: Long, playDuration: Long? = null) {
        try {
            // Check if this song was recently played (within last 10 seconds)
            // This is to avoid duplicate entries that are too close together
            val recentHistory = historyDao.getRecentHistoryForMusic(
                musicId,
                System.currentTimeMillis() - 10000 // 10 seconds ago
            )

            if (recentHistory.isEmpty()) {
                // If no recent entry, add a new one
                val history = PlayHistory(
                    musicId = musicId,
                    playedAt = System.currentTimeMillis(),
                    playDuration = playDuration
                )

                // Insert the history entry and get its ID
                val id = historyDao.insertHistory(history)
                Log.d(TAG, "Added new history for music ID: $musicId with history ID: $id")

                // After inserting, check if we need to remove old entries
                maintainHistoryLimit()
            } else {
                // If the song was recently played, just update the last entry
                val lastEntry = recentHistory.maxByOrNull { it.playedAt }
                lastEntry?.let {
                    historyDao.updateHistoryEntry(
                        it.id,
                        System.currentTimeMillis(),
                        playDuration ?: it.playDuration
                    )
                    Log.d(TAG, "Updated recent history for music ID: $musicId with history ID: ${it.id}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to history: ${e.message}", e)
        }
    }

    /**
     * Maintain only MAX_HISTORY_ENTRIES (6) in the history table by removing oldest entries
     */
    private suspend fun maintainHistoryLimit() {
        try {
            val count = historyDao.getHistoryCount()
            if (count > MAX_HISTORY_ENTRIES) {
                val toDelete = count - MAX_HISTORY_ENTRIES
                historyDao.deleteOldestEntries(toDelete)
                Log.d(TAG, "Deleted $toDelete oldest history entries to maintain limit of $MAX_HISTORY_ENTRIES")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error maintaining history limit: ${e.message}", e)
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

    // Get all history entries (now limited to 6 in the database)
    fun getAllHistory(): Flow<List<MusicWithHistory>> =
        historyDao.getHistoryWithMusic()

    // Get specifically 6 most recently played songs for home screen
    fun getRecentSixPlayed(): Flow<List<MusicWithHistory>> =
        historyDao.getRecentSixPlayed()

    // Old method, now ensures we get at most 6 entries
    fun getRecentlyPlayed(limit: Int = 6): Flow<List<MusicWithHistory>> =
        historyDao.getRecentlyPlayedWithMusic(limit)

    fun getMostPlayed(limit: Int = 6): Flow<List<MusicWithHistory>> =
        historyDao.getMostPlayedWithMusic(limit)
}