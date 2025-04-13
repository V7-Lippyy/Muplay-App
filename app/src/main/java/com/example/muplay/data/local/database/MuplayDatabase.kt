package com.example.muplay.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.muplay.data.local.database.dao.AlbumDao
import com.example.muplay.data.local.database.dao.ArtistDao
import com.example.muplay.data.local.database.dao.HistoryDao
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.local.database.dao.PlaylistDao
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.PlayHistory
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef

@Database(
    entities = [
        Music::class,
        Playlist::class,
        PlaylistMusicCrossRef::class,
        PlayHistory::class,
        Album::class,
        Artist::class
    ],
    version = 3, // Increased version number due to schema change
    exportSchema = false
)
abstract class MuplayDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
}