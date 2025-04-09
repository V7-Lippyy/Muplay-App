package com.example.muplay.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.muplay.data.local.database.dao.HistoryDao
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.local.database.dao.PlaylistDao
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.PlayHistory
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef

@Database(
    entities = [
        Music::class,
        Playlist::class,
        PlaylistMusicCrossRef::class,
        PlayHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MuplayDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
}