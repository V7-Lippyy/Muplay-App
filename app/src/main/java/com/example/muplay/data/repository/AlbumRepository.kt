package com.example.muplay.data.repository

import android.content.Context
import android.util.Log
import com.example.muplay.data.local.database.dao.AlbumDao
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Music
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlbumRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val albumDao: AlbumDao
) {
    private val TAG = "AlbumRepository"
    private val coverArtManager = CoverArtManager(context)

    // Get all albums
    fun getAllAlbums(): Flow<List<Album>> = albumDao.getAllAlbums()

    // Get album by name
    fun getAlbumByName(albumName: String): Flow<Album?> = albumDao.getAlbumByName(albumName)

    // Get music for an album
    fun getMusicByAlbum(albumName: String): Flow<List<Music>> = albumDao.getMusicByAlbum(albumName)

    // Get song count for an album
    fun getSongCountForAlbum(albumName: String): Flow<Int> = albumDao.getSongCountForAlbum(albumName)

    // Update album cover art
    suspend fun updateAlbumCover(albumName: String, coverArtPath: String?) {
        try {
            albumDao.updateAlbumCover(albumName, coverArtPath)
            Log.d(TAG, "Updated cover art for album $albumName: $coverArtPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating album cover art", e)
        }
    }

    // Create or update album
    suspend fun createOrUpdateAlbum(album: Album) {
        try {
            // Periksa apakah album sudah ada
            val existingAlbum = albumDao.getAlbumByName(album.name).firstOrNull()

            if (existingAlbum == null) {
                // Jika album tidak ada, tambahkan baru
                albumDao.insertAlbum(album)
                Log.d(TAG, "Created new album: ${album.name}")
            } else {
                // Jika album sudah ada, perbarui
                // Pertahankan coverArtPath yang ada jika yang baru null
                val updatedAlbum = if (album.coverArtPath == null && existingAlbum.coverArtPath != null) {
                    album.copy(coverArtPath = existingAlbum.coverArtPath)
                } else {
                    album
                }

                albumDao.updateAlbum(updatedAlbum)
                Log.d(TAG, "Updated album: ${album.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating album", e)
        }
    }

    // Refresh all albums from the music library
    suspend fun refreshAlbums(musicList: List<Music>) {
        try {
            // Group songs by album - filter out empty or "<unknown>" albums
            val albumGroups = musicList
                .filter { it.album.isNotBlank() && it.album != "<unknown>" }
                .groupBy { it.album.trim() }

            Log.d(TAG, "Found ${albumGroups.size} albums to process")

            // Get all existing albums first
            val existingAlbums = withContext(Dispatchers.IO) {
                albumDao.getAllAlbums().firstOrNull() ?: emptyList()
            }

            // Get song counts for each album to check which ones are empty
            val albumsWithCount = mutableMapOf<String, Int>()
            for (album in existingAlbums) {
                val count = withContext(Dispatchers.IO) {
                    albumDao.getSongCountForAlbum(album.name).firstOrNull() ?: 0
                }
                albumsWithCount[album.name] = count
            }

            // Create or update albums from music list
            albumGroups.forEach { (albumName, songs) ->
                if (albumName.isNotBlank() && albumName != "<unknown>") {
                    // Get the artist - use the most common artist in the album
                    val artistCounts = songs
                        .map { it.artist.trim() }
                        .filter { it.isNotBlank() && it != "<unknown>" }
                        .groupBy { it }
                        .mapValues { it.value.size }

                    val mostCommonArtist = artistCounts.maxByOrNull { it.value }?.key ?: ""

                    // Get the album art from the first song that has it
                    val albumArt = songs.firstOrNull { !it.albumArtPath.isNullOrEmpty() }?.albumArtPath

                    val album = Album(
                        name = albumName,
                        artist = mostCommonArtist,
                        coverArtPath = albumArt
                    )

                    createOrUpdateAlbum(album)

                    // Update count in our tracking map
                    albumsWithCount[albumName] = songs.size
                }
            }

            // Find albums to delete (empty or invalid)
            // 1. Find albums not in current music list (they will be empty)
            val albumsToDelete = albumsWithCount.filter { (name, count) ->
                count == 0 || // Album is empty
                        name.isBlank() || // Album name is blank
                        name == "<unknown>" // Album is unknown
            }.keys

            // 2. Delete albums that are empty or invalid
            if (albumsToDelete.isNotEmpty()) {
                Log.d(TAG, "Deleting ${albumsToDelete.size} empty or invalid albums: $albumsToDelete")

                for (albumName in albumsToDelete) {
                    albumDao.deleteAlbum(albumName)
                }
            }

            Log.d(TAG, "Refreshed ${albumGroups.size} albums, deleted ${albumsToDelete.size} empty albums")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing albums", e)
        }
    }

    // Clean up empty albums (can be called periodically or on demand)
    suspend fun cleanupEmptyAlbums() {
        try {
            // Get all albums
            val allAlbums = withContext(Dispatchers.IO) {
                albumDao.getAllAlbums().firstOrNull() ?: emptyList()
            }

            // Check each album for song count
            var emptyAlbumsCount = 0
            for (album in allAlbums) {
                val songCount = withContext(Dispatchers.IO) {
                    albumDao.getSongCountForAlbum(album.name).firstOrNull() ?: 0
                }

                // If album is empty, delete it
                if (songCount == 0 || album.name.isBlank() || album.name == "<unknown>") {
                    albumDao.deleteAlbum(album.name)
                    emptyAlbumsCount++
                    Log.d(TAG, "Deleted empty album: ${album.name}")
                }
            }

            Log.d(TAG, "Cleanup completed: deleted $emptyAlbumsCount empty albums")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up empty albums", e)
        }
    }
}