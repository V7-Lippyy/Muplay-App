package com.example.muplay.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// Model untuk relasi Playlist dan Music
data class PlaylistWithMusic(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistMusicCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "musicId"
        )
    )
    val songs: List<Music>
)

// Model untuk riwayat pemutaran lagu dengan data lagu terkait
data class MusicWithHistory(
    @Embedded val music: Music,
    val historyId: Long,
    val playedAt: Long,
    val playDuration: Long?
)