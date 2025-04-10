package com.example.muplay.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manager untuk menangani operasi gambar album
 */
class CoverArtManager(private val context: Context) {
    private val TAG = "CoverArtManager"

    // Direktori untuk menyimpan custom cover art secara permanen
    private val coverArtDirectory: File
        get() {
            val directory = File(context.filesDir, "cover_art")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            return directory
        }

    /**
     * Menyimpan URI gambar ke penyimpanan permanen
     * @param uri URI dari gambar yang akan disimpan
     * @param musicId ID musik untuk memastikan nama file yang unik
     * @return Path file yang disimpan, atau null jika gagal
     */
    suspend fun saveCoverArtFromUri(uri: Uri, musicId: Long): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)

                // Buat nama file yang unik berdasarkan musicId
                val fileName = "cover_${musicId}_${UUID.randomUUID()}.jpg"
                val file = File(coverArtDirectory, fileName)

                // Simpan gambar ke file
                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                Log.d(TAG, "Cover art tersimpan: ${file.absolutePath}")
                file.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error menyimpan cover art: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Memuat bitmap dari path file atau URI
     */
    suspend fun loadCoverArtBitmap(path: String?): Bitmap? {
        if (path == null) return null

        return withContext(Dispatchers.IO) {
            try {
                if (path.startsWith("/")) {
                    // Path file lokal
                    val file = File(path)
                    if (file.exists()) {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        Log.e(TAG, "File tidak ditemukan: $path")
                        null
                    }
                } else {
                    // URI content
                    val uri = Uri.parse(path)
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.use { BitmapFactory.decodeStream(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error memuat cover art: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Hapus semua cover art yang tidak digunakan lagi
     * (Ini bisa dipanggil secara periodik untuk membersihkan cache)
     */
    suspend fun cleanupUnusedCoverArt(usedPaths: List<String>) {
        withContext(Dispatchers.IO) {
            try {
                // Hanya hapus file yang tidak ada dalam daftar usedPaths
                coverArtDirectory.listFiles()?.forEach { file ->
                    if (!usedPaths.contains(file.absolutePath)) {
                        file.delete()
                        Log.d(TAG, "Hapus cover art yang tidak digunakan: ${file.absolutePath}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error membersihkan cover art: ${e.message}", e)
            }
        }
    }
}