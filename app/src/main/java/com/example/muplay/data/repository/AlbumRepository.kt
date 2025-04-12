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
            // Backup semua album yang ada
            val existingAlbums = withContext(Dispatchers.IO) {
                albumDao.getAllAlbums().firstOrNull() ?: emptyList()
            }

            // Group songs by album - filter out empty or "<unknown>" albums
            val albumGroups = musicList
                .filter { it.album.isNotBlank() && it.album != "<unknown>" }
                .groupBy { it.album.trim() }

            Log.d(TAG, "Found ${albumGroups.size} albums to process")

            // Create or update albums
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
                }
            }

            // Cek apakah ada album yang tidak valid (nama kosong) dan hapus
            val invalidAlbums = existingAlbums.filter { it.name.isBlank() || it.name == "<unknown>" }
            for (invalidAlbum in invalidAlbums) {
                // Hapus album tidak valid
                albumDao.deleteAlbum(invalidAlbum.name)
                Log.d(TAG, "Deleted invalid album: ${invalidAlbum.name}")
            }

            Log.d(TAG, "Refreshed ${albumGroups.size} albums")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing albums", e)
        }
    }
}