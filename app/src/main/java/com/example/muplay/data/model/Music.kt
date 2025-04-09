package com.example.muplay.data.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "music")
data class Music(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // dalam milidetik
    val uri: String,
    val albumArtPath: String? = null,
    val genre: String? = null,
    val trackNumber: Int? = null,
    val year: Int? = null,
    val dateAdded: Long? = null
)