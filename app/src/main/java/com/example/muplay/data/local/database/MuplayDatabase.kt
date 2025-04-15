package com.example.muplay.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.muplay.data.local.database.dao.AlbumDao
import com.example.muplay.data.local.database.dao.ArtistDao
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.local.database.dao.PlayCountDao
import com.example.muplay.data.local.database.dao.PlaylistDao
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Artist
import com.example.muplay.data.model.Music
import com.example.muplay.data.model.PlayCount
import com.example.muplay.data.model.Playlist
import com.example.muplay.data.model.PlaylistMusicCrossRef

@Database(
    entities = [
        Music::class,
        Playlist::class,
        PlaylistMusicCrossRef::class,
        PlayCount::class,
        Album::class,
        Artist::class
    ],
    version = 4, // Increased version number due to schema change from history to play counts
    exportSchema = false
)
abstract class MuplayDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playCountDao(): PlayCountDao
    abstract fun albumDao(): AlbumDao
    abstract fun artistDao(): ArtistDao
}