package com.example.muplay.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.example.muplay.data.local.database.MuplayDatabase
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
        ).build()
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
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}