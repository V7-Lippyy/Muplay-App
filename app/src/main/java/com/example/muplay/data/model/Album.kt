package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "album")
data class Album(
    @PrimaryKey
    val name: String,
    val artist: String,
    val coverArtPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)