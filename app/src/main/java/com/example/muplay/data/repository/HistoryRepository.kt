package com.example.muplay.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.muplay.data.local.database.dao.HistoryDao
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.data.model.PlayHistory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val historyDao: HistoryDao,
    private val musicDao: MusicDao
) {
    private val TAG = "HistoryRepository"
    private val MAX_HISTORY_ENTRIES = 6

    // SharedPreferences for backup storage of recently played IDs
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("muplay_history_prefs", Context.MODE_PRIVATE)
    }

    // In-memory cache of recent history
    private val _recentHistoryCache = MutableStateFlow<List<MusicWithHistory>>(emptyList())

    init {
        // Load any backup data when repository is created
        restoreFromBackup()
    }

    /**
     * Add a song to the history with robust error handling and backup storage.
     */
    suspend fun addToHistory(musicId: Long, playDuration: Long? = null) {
        // First update the cache and backup immediately for UI responsiveness
        updateCache(musicId)
        backupToPrefs(musicId)

        // Then do the full database operation
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Adding music ID $musicId to history database")

                // Check if this song was recently played (within last 10 seconds)
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

                // Ensure transaction commits
                kotlinx.coroutines.delay(100)

                // After DB operation, refresh the cache
                refreshCache()

            } catch (e: Exception) {
                Log.e(TAG, "Error adding to history: ${e.message}", e)

                // Retry with simplified approach
                try {
                    val history = PlayHistory(
                        musicId = musicId,
                        playedAt = System.currentTimeMillis(),
                        playDuration = playDuration
                    )
                    historyDao.insertHistory(history)
                    Log.d(TAG, "Retry succeeded: Added history for music ID: $musicId")
                    maintainHistoryLimit()
                    refreshCache()
                } catch (e2: Exception) {
                    Log.e(TAG, "Retry also failed. Error adding to history: ${e2.message}", e2)
                    // The backup in SharedPreferences will help in this case
                }
            }
        }
    }

    /**
     * Backup recently played music IDs to SharedPreferences for persistence
     */
    private suspend fun backupToPrefs(newMusicId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // Get current backup data
                val musicIds = getBackupMusicIds().toMutableList()

                // Add the new ID at the beginning (most recent)
                if (musicIds.contains(newMusicId)) {
                    musicIds.remove(newMusicId)
                }
                musicIds.add(0, newMusicId)

                // Keep only the most recent MAX_HISTORY_ENTRIES
                while (musicIds.size > MAX_HISTORY_ENTRIES) {
                    musicIds.removeAt(musicIds.size - 1)
                }

                // Save to SharedPreferences
                val musicIdsString = musicIds.joinToString(",")
                prefs.edit().putString("recent_music_ids", musicIdsString).apply()

                Log.d(TAG, "Backed up recent music IDs to SharedPreferences: $musicIdsString")
            } catch (e: Exception) {
                Log.e(TAG, "Error backing up to SharedPreferences: ${e.message}", e)
            }
        }
    }

    /**
     * Get backup music IDs from SharedPreferences
     */
    private fun getBackupMusicIds(): List<Long> {
        val musicIdsString = prefs.getString("recent_music_ids", "") ?: ""
        return if (musicIdsString.isNotEmpty()) {
            musicIdsString.split(",").mapNotNull { it.toLongOrNull() }
        } else {
            emptyList()
        }
    }

    /**
     * Restore from backup if needed
     */
    private fun restoreFromBackup() {
        Log.d(TAG, "Attempting to restore from backup")
        val musicIds = getBackupMusicIds()
        if (musicIds.isNotEmpty()) {
            Log.d(TAG, "Found backup music IDs: $musicIds")
        }
    }

    /**
     * Update the in-memory cache with a new history entry
     */
    private suspend fun updateCache(musicId: Long) {
        try {
            val music = musicDao.getMusicById(musicId).first() ?: return

            val history = MusicWithHistory(
                music = music,
                historyId = 0, // Temporary ID
                playedAt = System.currentTimeMillis(),
                playDuration = music.duration
            )

            // Update the cache by adding new entry at the beginning
            val currentCache = _recentHistoryCache.value.toMutableList()

            // Remove any existing entries for this music
            currentCache.removeAll { it.music.id == musicId }

            // Add at the beginning (most recent)
            currentCache.add(0, history)

            // Keep only the most recent MAX_HISTORY_ENTRIES
            while (currentCache.size > MAX_HISTORY_ENTRIES) {
                currentCache.removeAt(currentCache.size - 1)
            }

            _recentHistoryCache.value = currentCache

            Log.d(TAG, "Updated in-memory cache with music ID: $musicId")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating cache: ${e.message}", e)
        }
    }

    /**
     * Refresh the cache from the database
     */
    private suspend fun refreshCache() {
        withContext(Dispatchers.IO) {
            try {
                val recentHistory = historyDao.getRecentSixPlayed().first()
                _recentHistoryCache.value = recentHistory
                Log.d(TAG, "Refreshed cache with ${recentHistory.size} items from database")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing cache: ${e.message}", e)
            }
        }
    }

    /**
     * Maintain only MAX_HISTORY_ENTRIES in the history table by removing oldest entries
     */
    private suspend fun maintainHistoryLimit() {
        try {
            val count = historyDao.getHistoryCount()
            Log.d(TAG, "Current history count: $count, max allowed: $MAX_HISTORY_ENTRIES")

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
        withContext(Dispatchers.IO) {
            try {
                historyDao.clearAllHistory()
                Log.d(TAG, "Cleared all history")

                // Clear the cache and backup
                _recentHistoryCache.value = emptyList()
                prefs.edit().remove("recent_music_ids").apply()

            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history: ${e.message}", e)
            }
        }
    }

    suspend fun deleteHistoryEntry(historyId: Long) {
        withContext(Dispatchers.IO) {
            try {
                // First get the music ID for this history entry to update cache
                val history = historyDao.getHistoryWithMusic().first()
                    .find { it.historyId == historyId }

                // Delete from database
                historyDao.deleteHistoryById(historyId)
                Log.d(TAG, "Deleted history entry: $historyId")

                // Update cache
                history?.let {
                    val currentCache = _recentHistoryCache.value.toMutableList()
                    currentCache.removeAll { it.historyId == historyId }
                    _recentHistoryCache.value = currentCache

                    // Update backup
                    val musicIds = getBackupMusicIds().toMutableList()
                    musicIds.remove(it.music.id)
                    val musicIdsString = musicIds.joinToString(",")
                    prefs.edit().putString("recent_music_ids", musicIdsString).apply()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting history entry: ${e.message}", e)
            }
        }
    }

    suspend fun deleteHistoryForMusic(musicId: Long) {
        withContext(Dispatchers.IO) {
            try {
                historyDao.deleteHistoryForMusic(musicId)
                Log.d(TAG, "Deleted all history for music ID: $musicId")

                // Update cache
                val currentCache = _recentHistoryCache.value.toMutableList()
                currentCache.removeAll { it.music.id == musicId }
                _recentHistoryCache.value = currentCache

                // Update backup
                val musicIds = getBackupMusicIds().toMutableList()
                musicIds.remove(musicId)
                val musicIdsString = musicIds.joinToString(",")
                prefs.edit().putString("recent_music_ids", musicIdsString).apply()

            } catch (e: Exception) {
                Log.e(TAG, "Error deleting history for music: ${e.message}", e)
            }
        }
    }

    suspend fun removeDuplicateHistory() {
        withContext(Dispatchers.IO) {
            try {
                val duplicatesRemoved = historyDao.removeDuplicateEntries()
                Log.d(TAG, "Removed $duplicatesRemoved duplicate history entries")

                // Refresh cache after removing duplicates
                refreshCache()

            } catch (e: Exception) {
                Log.e(TAG, "Error removing duplicate history: ${e.message}", e)
            }
        }
    }

    /**
     * Get all history entries or fallback to cache if database query fails
     */
    fun getAllHistory(): Flow<List<MusicWithHistory>> {
        return historyDao.getHistoryWithMusic()
            .map { history ->
                if (history.isEmpty()) {
                    // If database returned empty, try to use cache
                    val cache = _recentHistoryCache.value
                    if (cache.isNotEmpty()) {
                        Log.d(TAG, "Using cache for getAllHistory, found ${cache.size} items")
                        cache
                    } else {
                        // If cache is also empty, try to restore from SharedPrefs
                        tryRestoreFromBackup()
                    }
                } else {
                    history
                }
            }
    }

    /**
     * Get specifically 6 most recently played songs for home screen
     * With multiple fallback mechanisms for resilience
     */
    fun getRecentSixPlayed(): Flow<List<MusicWithHistory>> {
        return historyDao.getRecentSixPlayed()
            .map { history ->
                // Filter untuk menghilangkan duplikasi berdasarkan ID lagu
                val uniqueHistory = history.distinctBy { it.music.id }

                if (uniqueHistory.isEmpty()) {
                    // If database returned empty, try to use cache
                    val cache = _recentHistoryCache.value
                    if (cache.isNotEmpty()) {
                        Log.d(TAG, "Using cache for getRecentSixPlayed, found ${cache.size} items")
                        // Filter cache juga untuk menghilangkan duplikasi
                        cache.distinctBy { it.music.id }
                    } else {
                        // If cache is also empty, try to restore from SharedPrefs
                        tryRestoreFromBackup()
                    }
                } else {
                    Log.d(TAG, "getRecentSixPlayed from database returned ${uniqueHistory.size} items")
                    // Update cache with this result (already filtered for uniqueness)
                    _recentHistoryCache.value = uniqueHistory
                    uniqueHistory
                }
            }
    }

    /**
     * Try to restore history from backup (SharedPreferences)
     */
    private suspend fun tryRestoreFromBackup(): List<MusicWithHistory> {
        return withContext(Dispatchers.IO) {
            try {
                val musicIds = getBackupMusicIds()
                Log.d(TAG, "Trying to restore history from backup, found ${musicIds.size} music IDs")

                if (musicIds.isEmpty()) return@withContext emptyList()

                val result = mutableListOf<MusicWithHistory>()

                // Get music details for each ID
                for (musicId in musicIds) {
                    val music = musicDao.getMusicById(musicId).first()
                    if (music != null) {
                        // Create a synthetic history entry
                        val history = MusicWithHistory(
                            music = music,
                            historyId = 0, // Temporary ID
                            playedAt = System.currentTimeMillis() - result.size * 1000, // Stagger times slightly
                            playDuration = music.duration
                        )
                        result.add(history)

                        // Also add to database for future queries
                        try {
                            val playHistory = PlayHistory(
                                musicId = musicId,
                                playedAt = history.playedAt,
                                playDuration = history.playDuration
                            )
                            historyDao.insertHistory(playHistory)
                        } catch (e: Exception) {
                            // Ignore database errors, we already have the data for display
                            Log.e(TAG, "Error inserting restored history to database: ${e.message}", e)
                        }
                    }
                }

                // Update the cache with restored data
                if (result.isNotEmpty()) {
                    _recentHistoryCache.value = result
                }

                Log.d(TAG, "Restored ${result.size} history items from backup")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring from backup: ${e.message}", e)
                emptyList()
            }
        }
    }

    // Get most recently played songs with a specified limit
    fun getRecentlyPlayed(limit: Int = 6): Flow<List<MusicWithHistory>> {
        return historyDao.getRecentlyPlayedWithMusic(limit)
            .map { history ->
                if (history.isEmpty() && limit <= MAX_HISTORY_ENTRIES) {
                    // Try cache for smaller limits
                    _recentHistoryCache.value
                } else {
                    history
                }
            }
    }

    // Get most played songs with a specified limit
    fun getMostPlayed(limit: Int = 6): Flow<List<MusicWithHistory>> =
        historyDao.getMostPlayedWithMusic(limit)
}