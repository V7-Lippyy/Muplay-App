package com.example.muplay.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.muplay.data.local.database.MuplayDatabase
import com.example.muplay.data.local.database.dao.AlbumDao
import com.example.muplay.data.local.database.dao.ArtistDao
import com.example.muplay.data.local.database.dao.HistoryDao
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.local.database.dao.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "muplay_preferences")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideMuplayDatabase(
        @ApplicationContext context: Context
    ): MuplayDatabase {
        return Room.databaseBuilder(
            context,
            MuplayDatabase::class.java,
            "muplay_database"
        )
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration() // Sebagai fallback jika migrasi gagal
            .build()
    }

    @Singleton
    @Provides
    fun provideMusicDao(database: MuplayDatabase): MusicDao {
        return database.musicDao()
    }

    @Singleton
    @Provides
    fun providePlaylistDao(database: MuplayDatabase): PlaylistDao {
        return database.playlistDao()
    }

    @Singleton
    @Provides
    fun provideHistoryDao(database: MuplayDatabase): HistoryDao {
        return database.historyDao()
    }

    @Singleton
    @Provides
    fun provideAlbumDao(database: MuplayDatabase): AlbumDao {
        return database.albumDao()
    }

    @Singleton
    @Provides
    fun provideArtistDao(database: MuplayDatabase): ArtistDao {
        return database.artistDao()
    }

    @Singleton
    @Provides
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }

    // Tambahkan migrasi untuk versi database
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Buat tabel album
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `album` (
                    `name` TEXT NOT NULL PRIMARY KEY,
                    `artist` TEXT NOT NULL,
                    `coverArtPath` TEXT,
                    `lastUpdated` INTEGER NOT NULL
                )
                """
            )

            // Buat tabel artist
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `artist` (
                    `name` TEXT NOT NULL PRIMARY KEY,
                    `coverArtPath` TEXT,
                    `lastUpdated` INTEGER NOT NULL
                )
                """
            )
        }
    }
}