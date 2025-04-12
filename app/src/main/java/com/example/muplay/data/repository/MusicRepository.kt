package com.example.muplay.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.muplay.data.local.database.dao.MusicDao
import com.example.muplay.data.model.Music
import com.example.muplay.util.CoverArtManager
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val dataStore: DataStore<Preferences>,
    private val lazyAlbumRepository: Lazy<AlbumRepository>,
    private val lazyArtistRepository: Lazy<ArtistRepository>
) {
    private val TAG = "MusicRepository"
    private val coverArtManager = CoverArtManager(context)

    companion object {
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val CUSTOM_COVER_ARTS_KEY = stringPreferencesKey("custom_cover_arts")
        private val EDITED_METADATA_KEY = stringPreferencesKey("edited_metadata")
    }

    // ===== Database Operations =====
    suspend fun refreshMusicLibrary() {
        try {
            val musicList = loadMusicFromDevice()
            if (musicList.isNotEmpty()) {
                // Save custom cover arts that already exist before deleting
                val customCoverArts = getCustomCoverArts()

                // Get edited metadata
                val editedMetadata = getEditedMetadata()

                // Update music with custom cover art and edited metadata if any
                val updatedMusicList = musicList.map { music ->
                    var updatedMusic = music

                    // Apply custom cover art if exists
                    val customCoverPath = customCoverArts[music.id.toString()]
                    if (customCoverPath != null && customCoverPath.isNotEmpty()) {
                        updatedMusic = updatedMusic.copy(albumArtPath = customCoverPath)
                    }

                    // Apply edited metadata if exists
                    val metadata = editedMetadata[music.id.toString()]
                    if (metadata != null) {
                        val parts = metadata.split("|")
                        if (parts.size >= 3) {
                            updatedMusic = updatedMusic.copy(
                                title = parts[0],
                                artist = parts[1],
                                album = parts[2]
                            )
                        }
                    }

                    updatedMusic
                }

                musicDao.deleteAll()
                musicDao.insertAll(updatedMusicList)

                // Refresh albums and artists
                lazyAlbumRepository.get().refreshAlbums(updatedMusicList)
                lazyArtistRepository.get().refreshArtists(updatedMusicList)

                Log.d(TAG, "Music library refreshed with ${updatedMusicList.size} songs")
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

                // Also save to preferences for persistence across app restarts
                saveCustomCoverArt(musicId.toString(), coverArtPath)

                Log.d(TAG, "Updated cover art for music ID $musicId: $coverArtPath")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating music cover art", e)
        }
    }

    // Update music metadata
    suspend fun updateMusicMetadata(musicId: Long, title: String, artist: String, album: String) {
        try {
            // Get the current music
            val music = getMusicById(musicId).first()

            if (music != null) {
                // 1. Update the music metadata in the database
                musicDao.updateMusicMetadata(musicId, title, artist, album)

                // 2. Save edited metadata to preferences
                saveEditedMetadata(musicId.toString(), "$title|$artist|$album")

                // 3. Update the MediaStore (External content provider)
                val success = updateMediaStoreMetadata(musicId, title, artist, album)
                Log.d(TAG, "MediaStore update status: $success")

                // 4. Refresh the music object to ensure it's updated
                musicDao.getMusicById(musicId).first()?.let { updatedMusic ->
                    Log.d(TAG, "Updated music in database: ${updatedMusic.title}, ${updatedMusic.artist}, ${updatedMusic.album}")
                }

                // 5. If artist or album has changed, refresh albums and artists
                if (music.artist != artist || music.album != album) {
                    // Get all music to refresh albums and artists
                    val allMusic = getAllMusic().first()
                    withContext(Dispatchers.IO) {
                        lazyAlbumRepository.get().refreshAlbums(allMusic)
                        lazyArtistRepository.get().refreshArtists(allMusic)
                    }
                }

                Log.d(TAG, "Updated metadata for music ID $musicId: title=$title, artist=$artist, album=$album")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating music metadata", e)
        }
    }

    // Save edited metadata to preferences for persistence
    private suspend fun saveEditedMetadata(musicId: String, metadata: String) {
        try {
            val editedMetadata = getEditedMetadata().toMutableMap()
            editedMetadata[musicId] = metadata

            // Convert to JSON string
            val jsonString = editedMetadata.entries.joinToString(separator = ",") {
                "\"${it.key}\":\"${it.value}\""
            }
            val jsonResult = "{$jsonString}"

            // Save to preferences
            dataStore.edit { preferences ->
                preferences[EDITED_METADATA_KEY] = jsonResult
            }

            Log.d(TAG, "Saved edited metadata to preferences for music ID $musicId: $metadata")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving edited metadata to preferences", e)
        }
    }

    // Get edited metadata from preferences
    private suspend fun getEditedMetadata(): Map<String, String> {
        return try {
            val jsonString = dataStore.data.map { preferences ->
                preferences[EDITED_METADATA_KEY] ?: "{}"
            }.first()

            parseJsonToMap(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting edited metadata from preferences", e)
            emptyMap()
        }
    }

    // Update the metadata in MediaStore
    private suspend fun updateMediaStoreMetadata(musicId: Long, title: String, artist: String, album: String): Boolean {
        return withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            try {
                // Create content values with the updated metadata
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.TITLE, title)
                    put(MediaStore.Audio.Media.ARTIST, artist)
                    put(MediaStore.Audio.Media.ALBUM, album)
                    // For displaying in the UI correctly
                    put(MediaStore.Audio.Media.DISPLAY_NAME, title)
                }

                // URI for the specific music track
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicId)

                Log.d(TAG, "Attempting to update MediaStore for URI: $uri")

                // Update the MediaStore
                val updated = contentResolver.update(uri, values, null, null)
                if (updated > 0) {
                    Log.d(TAG, "MediaStore updated successfully for music ID: $musicId")
                    true
                } else {
                    Log.w(TAG, "MediaStore update failed for music ID: $musicId")
                    false
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating MediaStore metadata", e)
                false
            }
        }
    }

    // Save custom cover art path to preferences
    private suspend fun saveCustomCoverArt(musicId: String, coverArtPath: String) {
        try {
            val customCoverArts = getCustomCoverArts().toMutableMap()
            customCoverArts[musicId] = coverArtPath

            // Convert to JSON string
            val jsonString = customCoverArts.entries.joinToString(separator = ",") {
                "\"${it.key}\":\"${it.value}\""
            }
            val jsonResult = "{$jsonString}"

            // Save to preferences
            dataStore.edit { preferences ->
                preferences[CUSTOM_COVER_ARTS_KEY] = jsonResult
            }

            Log.d(TAG, "Saved custom cover art to preferences for music ID $musicId")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom cover art to preferences", e)
        }
    }

    // Get all custom cover arts from preferences
    private suspend fun getCustomCoverArts(): Map<String, String> {
        return try {
            val jsonString = dataStore.data.map { preferences ->
                preferences[CUSTOM_COVER_ARTS_KEY] ?: "{}"
            }.first()

            parseJsonToMap(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting custom cover arts from preferences", e)
            emptyMap()
        }
    }

    // Helper function to parse JSON string to Map
    private fun parseJsonToMap(jsonString: String): Map<String, String> {
        return if (jsonString == "{}") {
            emptyMap()
        } else {
            val result = mutableMapOf<String, String>()
            val content = jsonString.trim().removeSurrounding("{", "}")
            if (content.isNotEmpty()) {
                content.split(",").forEach { pair ->
                    val keyValue = pair.split(":", limit = 2)
                    if (keyValue.size == 2) {
                        val key = keyValue[0].trim().removeSurrounding("\"")
                        val value = keyValue[1].trim().removeSurrounding("\"")
                        result[key] = value
                    }
                }
            }
            result
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

    // Manually force refresh album and artist data
    suspend fun forceRefreshCollections() {
        try {
            val allMusic = getAllMusic().first()
            if (allMusic.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    lazyAlbumRepository.get().refreshAlbums(allMusic)
                    lazyArtistRepository.get().refreshArtists(allMusic)
                }
                Log.d(TAG, "Force refreshed album and artist collections")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error force refreshing collections", e)
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
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.TRACK
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
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val trackNumberColumn = try {
                cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
            } catch (e: Exception) {
                -1
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn)
                var artist = cursor.getString(artistColumn)
                var album = cursor.getString(albumColumn)
                val duration = cursor.getLong(durationColumn)
                val albumId = cursor.getLong(albumIdColumn)

                // Handle <unknown> values
                if (artist == "<unknown>") artist = ""
                if (album == "<unknown>") album = ""

                val year = if (yearColumn != -1) {
                    try { cursor.getInt(yearColumn) } catch (e: Exception) { null }
                } else null

                val dateAdded = if (dateAddedColumn != -1) {
                    try { cursor.getLong(dateAddedColumn) } catch (e: Exception) { null }
                } else null

                val trackNumber = if (trackNumberColumn != -1) {
                    try { cursor.getInt(trackNumberColumn) } catch (e: Exception) { null }
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
                    dateAdded = dateAdded,
                    trackNumber = trackNumber
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