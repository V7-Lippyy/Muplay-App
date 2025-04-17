package com.example.muplay.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.muplay.data.local.database.dao.LyricDao
import com.example.muplay.data.model.Lyric
import com.example.muplay.data.model.LrcContent
import com.example.muplay.data.model.LyricLine
import com.example.muplay.util.LrcParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricDao: LyricDao
) {
    private val TAG = "LyricRepository"

    /**
     * Get lyrics for a specific song
     */
    fun getLyricForMusic(musicId: Long): Flow<Lyric?> {
        return lyricDao.getLyricForMusic(musicId)
    }

    /**
     * Check if lyrics exist for a song
     */
    suspend fun hasLyrics(musicId: Long): Boolean {
        return lyricDao.hasLyrics(musicId)
    }

    /**
     * Add lyrics from an LRC file URI
     */
    suspend fun addLyricsFromUri(musicId: Long, uri: Uri): Boolean {
        return try {
            // Parse the LRC file
            val lrcContent = LrcParser.parseLrcFromUri(context, uri)

            // Save LRC content to internal storage
            val fileName = "lyric_${musicId}.lrc"
            val savedPath = LrcParser.saveLrcToFile(context, lrcContent, fileName)

            if (savedPath != null) {
                // Check if lyrics already exist for this music
                if (lyricDao.hasLyrics(musicId)) {
                    // Update existing lyrics
                    lyricDao.updateLrcPath(musicId, savedPath)
                } else {
                    // Create new lyrics entry
                    val lyric = Lyric(
                        musicId = musicId,
                        lrcPath = savedPath
                    )
                    lyricDao.insertLyric(lyric)
                }
                Log.d(TAG, "Added lyrics for music ID: $musicId, path: $savedPath")
                true
            } else {
                Log.e(TAG, "Failed to save LRC file for music ID: $musicId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding lyrics from URI: ${e.message}", e)
            false
        }
    }

    /**
     * Delete lyrics for a song
     */
    suspend fun deleteLyrics(musicId: Long) {
        try {
            // Get the lyrics to delete the file
            val lyric = lyricDao.getLyricForMusic(musicId).firstOrNull()

            // Delete the LRC file if exists
            if (lyric?.lrcPath != null) {
                try {
                    val file = File(lyric.lrcPath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting LRC file: ${e.message}", e)
                }
            }

            // Delete from database
            lyricDao.deleteLyric(musicId)
            Log.d(TAG, "Deleted lyrics for music ID: $musicId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting lyrics: ${e.message}", e)
        }
    }

    /**
     * Load parsed LRC content for a song
     */
    suspend fun loadLrcContent(musicId: Long): LrcContent {
        return withContext(Dispatchers.IO) {
            try {
                val lyric = lyricDao.getLyricForMusic(musicId).firstOrNull()
                if (lyric?.lrcPath != null) {
                    LrcParser.parseLrcFromFile(lyric.lrcPath)
                } else {
                    LrcContent()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading LRC content: ${e.message}", e)
                LrcContent()
            }
        }
    }

    /**
     * Get the current lyric line based on playback position
     */
    suspend fun getCurrentLyricLine(lrcContent: LrcContent, position: Long): LyricLine? {
        val lines = lrcContent.lines
        if (lines.isEmpty()) return null

        // Find the current line based on position
        var currentIndex = -1
        for (i in lines.indices) {
            if (i == lines.size - 1 || (lines[i].timestamp <= position && (i + 1 < lines.size && lines[i + 1].timestamp > position))) {
                currentIndex = i
                break
            }
        }

        // If no matching line found and position is after the last line
        if (currentIndex == -1 && lines.isNotEmpty() && position >= lines.last().timestamp) {
            currentIndex = lines.size - 1
        }

        return if (currentIndex >= 0 && currentIndex < lines.size) {
            lines[currentIndex].copy(isCurrent = true)
        } else {
            null
        }
    }

    /**
     * Get lyrics with highlighted current line
     */
    suspend fun getSynchronizedLyrics(lrcContent: LrcContent, position: Long): List<LyricLine> {
        val lines = lrcContent.lines
        if (lines.isEmpty()) return emptyList()

        var currentIndex = -1

        // Find the current line
        for (i in lines.indices) {
            if (i == lines.size - 1 || (lines[i].timestamp <= position && (i + 1 < lines.size && lines[i + 1].timestamp > position))) {
                currentIndex = i
                break
            }
        }

        // If position is past the last line
        if (currentIndex == -1 && lines.isNotEmpty() && position >= lines.last().timestamp) {
            currentIndex = lines.size - 1
        }

        // Create a list with updated current status
        return lines.mapIndexed { index, line ->
            line.copy(isCurrent = index == currentIndex)
        }
    }
}