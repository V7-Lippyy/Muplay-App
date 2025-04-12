package com.example.muplay.data.repository

import android.content.Context
import android.util.Log
import com.example.muplay.data.local.database.dao.ArtistDao
import com.example.muplay.data.model.Artist
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
class ArtistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val artistDao: ArtistDao
) {
    private val TAG = "ArtistRepository"
    private val coverArtManager = CoverArtManager(context)

    // Get all artists
    fun getAllArtists(): Flow<List<Artist>> = artistDao.getAllArtists()

    // Get artist by name
    fun getArtistByName(artistName: String): Flow<Artist?> = artistDao.getArtistByName(artistName)

    // Get music for an artist
    fun getMusicByArtist(artistName: String): Flow<List<Music>> = artistDao.getMusicByArtist(artistName)

    // Get song count for an artist
    fun getSongCountForArtist(artistName: String): Flow<Int> = artistDao.getSongCountForArtist(artistName)

    // Update artist cover art
    suspend fun updateArtistCover(artistName: String, coverArtPath: String?) {
        try {
            artistDao.updateArtistCover(artistName, coverArtPath)
            Log.d(TAG, "Updated cover art for artist $artistName: $coverArtPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating artist cover art", e)
        }
    }

    // Create or update artist
    suspend fun createOrUpdateArtist(artist: Artist) {
        try {
            // Periksa apakah artist sudah ada
            val existingArtist = artistDao.getArtistByName(artist.name).firstOrNull()

            if (existingArtist == null) {
                // Jika artist tidak ada, tambahkan baru
                artistDao.insertArtist(artist)
                Log.d(TAG, "Created new artist: ${artist.name}")
            } else {
                // Jika artist sudah ada, perbarui
                // Pertahankan coverArtPath yang ada jika yang baru null
                val updatedArtist = if (artist.coverArtPath == null && existingArtist.coverArtPath != null) {
                    artist.copy(coverArtPath = existingArtist.coverArtPath)
                } else {
                    artist
                }

                artistDao.updateArtist(updatedArtist)
                Log.d(TAG, "Updated artist: ${artist.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating artist", e)
        }
    }

    // Refresh all artists from the music library
    suspend fun refreshArtists(musicList: List<Music>) {
        try {
            // Backup semua artist yang ada
            val existingArtists = withContext(Dispatchers.IO) {
                artistDao.getAllArtists().firstOrNull() ?: emptyList()
            }

            // Group songs by artist - filter out empty or "<unknown>" artists
            val artistGroups = musicList
                .filter { it.artist.isNotBlank() && it.artist != "<unknown>" }
                .groupBy { it.artist.trim() }

            Log.d(TAG, "Found ${artistGroups.size} artists to process")

            // Create or update artists
            artistGroups.forEach { (artistName, songs) ->
                if (artistName.isNotBlank() && artistName != "<unknown>") {
                    // Get the album art from the first song that has it to use as artist image
                    val artistArt = songs.firstOrNull { !it.albumArtPath.isNullOrEmpty() }?.albumArtPath

                    val artist = Artist(
                        name = artistName,
                        coverArtPath = artistArt
                    )

                    createOrUpdateArtist(artist)
                }
            }

            // Cek apakah ada artist yang tidak valid (nama kosong) dan hapus
            val invalidArtists = existingArtists.filter { it.name.isBlank() || it.name == "<unknown>" }
            for (invalidArtist in invalidArtists) {
                // Hapus artist tidak valid
                artistDao.deleteArtist(invalidArtist.name)
                Log.d(TAG, "Deleted invalid artist: ${invalidArtist.name}")
            }

            Log.d(TAG, "Refreshed ${artistGroups.size} artists")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing artists", e)
        }
    }
}