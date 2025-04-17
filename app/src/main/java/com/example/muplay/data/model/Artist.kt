package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artist")
data class Artist(
    @PrimaryKey
    val name: String,
    val coverArtPath: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)