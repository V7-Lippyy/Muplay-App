package com.example.muplay.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.muplay.data.model.LrcContent
import com.example.muplay.data.model.LrcMetadata
import com.example.muplay.data.model.LyricLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

/**
 * Utility class for parsing LRC (Lyrics) files.
 */
class LrcParser {
    companion object {
        private const val TAG = "LrcParser"

        // Regex for timestamp pattern [mm:ss.xx]
        private val TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\]")

        // Regex for metadata tags [xx:yyyy]
        private val METADATA_PATTERN = Pattern.compile("\\[(\\w+):([^\\]]+)\\]")

        /**
         * Parse an LRC file from a Uri.
         */
        suspend fun parseLrcFromUri(context: Context, uri: Uri): LrcContent {
            return withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { stream ->
                        val reader = BufferedReader(InputStreamReader(stream))
                        return@withContext parseLrcContent(reader)
                    }
                    LrcContent()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing LRC from URI: ${e.message}", e)
                    LrcContent()
                }
            }
        }

        /**
         * Parse an LRC file from a file path.
         */
        suspend fun parseLrcFromFile(filePath: String): LrcContent {
            return withContext(Dispatchers.IO) {
                try {
                    val file = File(filePath)
                    if (!file.exists()) {
                        Log.e(TAG, "LRC file does not exist: $filePath")
                        return@withContext LrcContent()
                    }

                    val reader = BufferedReader(InputStreamReader(FileInputStream(file)))
                    return@withContext parseLrcContent(reader)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing LRC file: ${e.message}", e)
                    LrcContent()
                }
            }
        }

        /**
         * Parse LRC content from a reader.
         */
        private fun parseLrcContent(reader: BufferedReader): LrcContent {
            val metadata = LrcMetadata()
            val lyrics = mutableListOf<LyricLine>()
            var offset = 0

            reader.useLines { lines ->
                lines.forEach { line ->
                    if (line.trim().isEmpty()) return@forEach

                    // Check for metadata
                    val metadataMatcher = METADATA_PATTERN.matcher(line)
                    if (metadataMatcher.matches()) {
                        val tag = metadataMatcher.group(1)?.lowercase()
                        val value = metadataMatcher.group(2)

                        when (tag) {
                            "ti" -> metadata.title
                            "ar" -> metadata.artist
                            "al" -> metadata.album
                            "by" -> metadata.by
                            "offset" -> try {
                                offset = value?.toIntOrNull() ?: 0
                            } catch (e: NumberFormatException) {
                                Log.e(TAG, "Invalid offset value: $value")
                            }
                        }
                        return@forEach
                    }

                    // Parse timestamps
                    val timestampMatcher = TIMESTAMP_PATTERN.matcher(line)
                    var text = line
                    val timestamps = mutableListOf<Long>()

                    while (timestampMatcher.find()) {
                        val minutes = timestampMatcher.group(1)?.toIntOrNull() ?: 0
                        val seconds = timestampMatcher.group(2)?.toIntOrNull() ?: 0
                        val milliseconds = timestampMatcher.group(3)?.let {
                            if (it.length == 2) it.toIntOrNull() ?: 0 * 10 else it.toIntOrNull() ?: 0
                        } ?: 0

                        val timestamp = (minutes * 60 * 1000L + seconds * 1000L + milliseconds) + offset
                        timestamps.add(timestamp)

                        // Remove the timestamp from the text
                        text = text.replaceFirst(timestampMatcher.group(0) ?: "", "")
                    }

                    // If we found timestamps, add each one with the corresponding text
                    if (timestamps.isNotEmpty()) {
                        timestamps.forEach { timestamp ->
                            lyrics.add(LyricLine(timestamp, text.trim()))
                        }
                    }
                }
            }

            // Sort lyrics by timestamp
            val sortedLyrics = lyrics.sortedBy { it.timestamp }
            return LrcContent(
                metadata = LrcMetadata(
                    title = metadata.title,
                    artist = metadata.artist,
                    album = metadata.album,
                    by = metadata.by,
                    offset = offset
                ),
                lines = sortedLyrics
            )
        }

        /**
         * Save LRC content to a file.
         */
        suspend fun saveLrcToFile(context: Context, content: LrcContent, fileName: String): String? {
            return withContext(Dispatchers.IO) {
                try {
                    val lrcDirectory = File(context.filesDir, "lyrics")
                    if (!lrcDirectory.exists()) {
                        lrcDirectory.mkdirs()
                    }

                    val file = File(lrcDirectory, fileName)
                    file.bufferedWriter().use { writer ->
                        // Write metadata
                        content.metadata.title?.let { writer.write("[ti:$it]\n") }
                        content.metadata.artist?.let { writer.write("[ar:$it]\n") }
                        content.metadata.album?.let { writer.write("[al:$it]\n") }
                        content.metadata.by?.let { writer.write("[by:$it]\n") }
                        if (content.metadata.offset != 0) {
                            writer.write("[offset:${content.metadata.offset}]\n")
                        }
                        writer.write("\n")

                        // Write lyrics
                        content.lines.forEach { lyricLine ->
                            val minutes = (lyricLine.timestamp / 60000).toString().padStart(2, '0')
                            val seconds = ((lyricLine.timestamp % 60000) / 1000).toString().padStart(2, '0')
                            val milliseconds = ((lyricLine.timestamp % 1000) / 10).toString().padStart(2, '0')
                            writer.write("[$minutes:$seconds.$milliseconds]${lyricLine.text}\n")
                        }
                    }

                    return@withContext file.absolutePath
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving LRC file: ${e.message}", e)
                    null
                }
            }
        }
    }
}