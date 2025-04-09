package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "play_history",
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
data class PlayHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val musicId: Long,
    val playedAt: Long, // timestamp kapan diputar
    val playDuration: Long? = null // berapa lama diputar (dalam milidetik)
)