package com.example.muplay.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.model.Music
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val dataStore: DataStore<Preferences>
) {
    private val TAG = "MusicRepository"

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val CUSTOM_COVER_ARTS = "custom_cover_arts"
    }

    // ===== Operasi Database =====
    suspend fun refreshMusicLibrary() {
        try {
            val musicList = loadMusicFromDevice()
            if (musicList.isNotEmpty()) {
                musicDao.deleteAll()
                musicDao.insertAll(musicList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing music library", e)
        }
    }

    fun getAllMusic(): Flow<List<Music>> = musicDao.getAllMusic()

    fun getMusicById(id: Long): Flow<Music?> = musicDao.getMusicById(id)

    fun searchMusic(query: String): Flow<List<Music>> =
        musicDao.searchMusic("%$query%")

    fun getMusicByArtist(artist: String): Flow<List<Music>> =
        musicDao.getMusicByArtist(artist)

    fun getMusicByGenre(genre: String): Flow<List<Music>> =
        musicDao.getMusicByGenre(genre)

    fun getDistinctArtists(): Flow<List<String>> = musicDao.getDistinctArtists()

    fun getDistinctGenres(): Flow<List<String>> = musicDao.getDistinctGenres()

    // Update music with custom cover art
    suspend fun updateMusicCoverArt(musicId: Long, coverArtPath: String) {
        try {
            // Get the music from database
            val music = getMusicById(musicId).first()

            // Update the music object with new cover art
            music?.let {
                val updatedMusic = it.copy(albumArtPath = coverArtPath)

                // Save to database
                musicDao.updateMusic(updatedMusic)

                Log.d(TAG, "Updated cover art for music ID $musicId: $coverArtPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating music cover art", e)
        }
    }

    // ===== Preferences =====
    fun getDarkThemePreference(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[DARK_THEME_KEY] ?: false
        }
    }

    suspend fun setDarkThemePreference(isDarkTheme: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = isDarkTheme
        }
    }

    // ===== Media Store Query =====
    private suspend fun loadMusicFromDevice(): List<Music> {
        val musicList = mutableListOf<Music>()
        val contentResolver = context.contentResolver

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ?"
        val selectionArgs = arrayOf("1")
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val yearColumn = cursor.getColumnIndex(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val album = cursor.getString(albumColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                val year = if (yearColumn != -1) {
                    try { cursor.getInt(yearColumn) } catch (e: Exception) { null }
                } else null

                val dateAdded = if (dateAddedColumn != -1) {
                    try { cursor.getLong(dateAddedColumn) } catch (e: Exception) { null }
                } else null

                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )

                // Album art URI
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId
                )

                // Genre detection
                val genre = getGenreFromAudio(contentResolver, id)

                val music = Music(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    uri = contentUri.toString(),
                    albumArtPath = albumArtUri.toString(),
                    genre = genre,
                    year = year,
                    dateAdded = dateAdded
                )

                musicList.add(music)
            }
        }

        return musicList
    }

    private fun getGenreFromAudio(contentResolver: ContentResolver, audioId: Long): String? {
        var genre: String? = null

        try {
            val uri = MediaStore.Audio.Genres.getContentUriForAudioId("external", audioId.toInt())
            val projection = arrayOf(MediaStore.Audio.Genres.NAME)

            contentResolver.query(
                uri,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    genre = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting genre", e)
        }

        // Fallback: menggunakan MediaMetadataRetriever jika diperlukan
        if (genre == null) {
            try {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audioId
                )

                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(context, uri)
                    genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error extracting genre metadata", e)
            }
        }

        return genre
    }
}