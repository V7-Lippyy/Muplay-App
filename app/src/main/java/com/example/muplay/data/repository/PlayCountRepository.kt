package com.example.muplay.data.repository

import android.content.Context
import android.util.Log
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.local.database.dao.PlayCountDao
import com.example.muplay.data.model.MusicWithPlayCount
import com.example.muplay.data.model.PlayCount
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayCountRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playCountDao: PlayCountDao,
    private val musicDao: MusicDao
) {
    private val TAG = "PlayCountRepository"

    /**
     * Increment the play count for a song.
     * Creates a new record if one doesn't exist.
     */
    suspend fun incrementPlayCount(musicId: Long) {
        try {
            val hasCount = withContext(Dispatchers.IO) {
                playCountDao.hasPlayCount(musicId)
            }

            if (hasCount > 0) {
                // Existing record - increment count
                playCountDao.incrementCount(musicId)
                Log.d(TAG, "Incremented play count for music ID: $musicId")
            } else {
                // Create new record with count = 1
                val playCount = PlayCount(
                    musicId = musicId,
                    count = 1,
                    lastPlayed = System.currentTimeMillis()
                )
                playCountDao.insertPlayCount(playCount)
                Log.d(TAG, "Created new play count for music ID: $musicId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing play count: ${e.message}", e)
        }
    }

    /**
     * Get the play count for a specific music track
     */
    fun getPlayCountForMusic(musicId: Long): Flow<PlayCount?> =
        playCountDao.getPlayCountForMusic(musicId)

    /**
     * Get the most played songs (with play count of minCount or more)
     */
    fun getMostPlayedSongs(limit: Int = 9, minCount: Int = 5): Flow<List<MusicWithPlayCount>> =
        playCountDao.getMostPlayedMusic(minCount, limit)
            .flowOn(Dispatchers.IO)

    /**
     * Get the top played songs regardless of play count
     */
    fun getTopPlayedSongs(limit: Int = 9): Flow<List<MusicWithPlayCount>> =
        playCountDao.getTopPlayedMusic(limit)
            .flowOn(Dispatchers.IO)

    /**
     * Get favorite songs
     */
    fun getFavoriteSongs(limit: Int = 50): Flow<List<MusicWithPlayCount>> =
        playCountDao.getFavoriteSongs(limit)
            .flowOn(Dispatchers.IO)

    /**
     * Set a song as favorite
     */
    suspend fun setFavorite(musicId: Long, isFavorite: Boolean) {
        try {
            val hasCount = withContext(Dispatchers.IO) {
                playCountDao.hasPlayCount(musicId)
            }

            if (hasCount > 0) {
                // Update existing record
                playCountDao.setFavorite(musicId, isFavorite)
                Log.d(TAG, "Updated favorite status for music ID: $musicId to $isFavorite")
            } else {
                // Create new record with favorite status
                val playCount = PlayCount(
                    musicId = musicId,
                    count = 0,
                    lastPlayed = System.currentTimeMillis(),
                    isFavorite = isFavorite
                )
                playCountDao.insertPlayCount(playCount)
                Log.d(TAG, "Created new play count with favorite status for music ID: $musicId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting favorite status: ${e.message}", e)
        }
    }

    /**
     * Delete all play count data - useful for resetting
     */
    suspend fun clearAllPlayCounts() {
        try {
            playCountDao.deleteAllPlayCounts()
            Log.d(TAG, "Cleared all play counts")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing play counts: ${e.message}", e)
        }
    }
}