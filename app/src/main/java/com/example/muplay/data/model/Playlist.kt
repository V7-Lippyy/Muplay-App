package com.example.muplay.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val coverArtPath: String? = null
)

// Relasi antara playlist dan musik
@Entity(
    tableName = "playlist_music_cross_ref",
    primaryKeys = ["playlistId", "musicId"],
    indices = [Index(value = ["musicId"])]
)
data class PlaylistMusicCrossRef(
    val playlistId: Long,
    val musicId: Long,
    val position: Int // Posisi lagu dalam playlist
)