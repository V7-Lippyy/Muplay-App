package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing lyrics for a music track.
 */
@Entity(
    tableName = "lyric",
    foreignKeys = [
        ForeignKey(
            entity = Music::class,
            parentColumns = ["id"],
            childColumns = ["musicId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("musicId")]
)
data class Lyric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val musicId: Long,
    val lrcPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Represents a single line of lyrics with its timestamp.
 */
data class LyricLine(
    val timestamp: Long, // Timestamp in milliseconds
    val text: String,
    val isCurrent: Boolean = false
)

/**
 * Represents metadata from an LRC file.
 */
data class LrcMetadata(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val by: String? = null,  // Creator of the LRC file
    val offset: Int = 0      // Global time adjustment in milliseconds
)

/**
 * Represents a complete parsed LRC file.
 */
data class LrcContent(
    val metadata: LrcMetadata = LrcMetadata(),
    val lines: List<LyricLine> = emptyList()
)