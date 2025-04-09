package com.example.muplay.util

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object MediaUtil {
    private const val TAG = "MediaUtil"

    /**
     * Mengambil album art untuk lagu tertentu
     */
    suspend fun getAlbumArt(context: Context, albumId: Long): ImageBitmap? {
        return try {
            val albumArtUri = ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId
            )

            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(albumArtUri)
                .allowHardware(false)
                .build()

            val drawable = withContext(Dispatchers.IO) {
                loader.execute(request).drawable
            }

            drawable?.toBitmap()?.asImageBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading album art", e)
            null
        }
    }

    /**
     * Mengambil album art dari file lagu menggunakan MediaMetadataRetriever
     */
    suspend fun getAlbumArtFromFile(context: Context, uri: Uri): ImageBitmap? {
        return try {
            withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)

                val art = retriever.embeddedPicture
                retriever.release()

                if (art != null) {
                    android.graphics.BitmapFactory.decodeByteArray(art, 0, art.size).asImageBitmap()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting embedded album art", e)
            null
        }
    }

    /**
     * Menyimpan album art yang dipilih user ke penyimpanan internal aplikasi
     */
    suspend fun saveCustomAlbumArt(context: Context, uri: Uri, fileName: String): String? {
        return try {
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val outputFile = File(context.filesDir, fileName)
                    FileOutputStream(outputFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()
                    outputFile.absolutePath
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving custom album art", e)
            null
        }
    }

    /**
     * Mendapatkan total durasi dari daftar lagu
     */
    fun getTotalDuration(songs: List<com.example.muplay.data.model.Music>): Long {
        return songs.sumOf { it.duration }
    }
}