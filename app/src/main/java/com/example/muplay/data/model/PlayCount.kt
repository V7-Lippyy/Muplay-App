package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.PrimaryKey

/**
 * Model to track how many times a song has been played
 */
@Entity(tableName = "play_count")
data class PlayCount(
    @PrimaryKey
    val musicId: Long,
    val count: Int = 0,
    val lastPlayed: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

/**
 * Represents a music item with its play count for UI display
 */
data class MusicWithPlayCount(
    @Embedded val music: Music,
    val playCount: Int,
    val lastPlayed: Long,
    val isFavorite: Boolean
)