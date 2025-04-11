package com.example.muplay.data.repository

import android.content.Context
import android.util.Log
import com.example.muplay.data.local.database.dao.AlbumDao
import com.example.muplay.data.model.Album
import com.example.muplay.data.model.Music
import com.example.muplay.util.CoverArtManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
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
            val existingAlbum = albumDao.getAlbumByName(album.name).hashCode()
            if (existingAlbum == 0) {
                albumDao.insertAlbum(album)
                Log.d(TAG, "Created new album: ${album.name}")
            } else {
                albumDao.updateAlbum(album)
                Log.d(TAG, "Updated album: ${album.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating album", e)
        }
    }

    // Refresh all albums from the music library
    suspend fun refreshAlbums(musicList: List<Music>) {
        try {
            // Group songs by album
            val albumGroups = musicList.groupBy { it.album }

            // Create or update albums
            albumGroups.forEach { (albumName, songs) ->
                if (albumName.isNotBlank()) {
                    // Get the artist - use the most common artist in the album
                    val artistCounts = songs.groupBy { it.artist }
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

            Log.d(TAG, "Refreshed ${albumGroups.size} albums")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing albums", e)
        }
    }
}