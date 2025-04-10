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
                val fileName = "cover_${musicId}.jpg"
                val file = File(coverArtDirectory, fileName)

                // Jika file sudah ada, hapus terlebih dahulu
                if (file.exists()) {
                    file.delete()
                }

                // Simpan gambar ke file
                inputStream?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                }

                // Pastikan file berhasil dibuat
                if (!file.exists() || file.length() == 0L) {
                    Log.e(TAG, "Failed to create cover art file")
                    return@withContext null
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
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            Log.d(TAG, "Successfully loaded bitmap from file: $path")
                            bitmap
                        } else {
                            Log.e(TAG, "Failed to decode bitmap from file: $path")
                            null
                        }
                    } else {
                        Log.e(TAG, "File tidak ditemukan: $path")
                        null
                    }
                } else {
                    // URI content
                    try {
                        val uri = Uri.parse(path)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        inputStream?.use {
                            val bitmap = BitmapFactory.decodeStream(it)
                            if (bitmap != null) {
                                Log.d(TAG, "Successfully loaded bitmap from URI: $path")
                                bitmap
                            } else {
                                Log.e(TAG, "Failed to decode bitmap from URI: $path")
                                null
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading from URI: ${e.message}", e)
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error memuat cover art: ${e.message}", e)
                null
            }
        }
    }

    /**
     * Memeriksa apakah file cover art ada
     */
    suspend fun coverArtExists(path: String?): Boolean {
        if (path == null) return false

        return withContext(Dispatchers.IO) {
            try {
                if (path.startsWith("/")) {
                    val file = File(path)
                    val exists = file.exists() && file.length() > 0
                    Log.d(TAG, "Cover art file exists: $exists, path: $path")
                    exists
                } else {
                    // Untuk URI, kita coba akses untuk memeriksa
                    try {
                        val uri = Uri.parse(path)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val exists = inputStream != null
                        inputStream?.close()
                        Log.d(TAG, "Cover art URI exists: $exists, path: $path")
                        exists
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking URI: ${e.message}", e)
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error memeriksa cover art: ${e.message}", e)
                false
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
                        val deleted = file.delete()
                        Log.d(TAG, "Hapus cover art yang tidak digunakan: ${file.absolutePath}, berhasil: $deleted")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error membersihkan cover art: ${e.message}", e)
            }
        }
    }

    /**
     * Mendapatkan semua file cover art
     */
    suspend fun getAllCoverArtFiles(): List<File> {
        return withContext(Dispatchers.IO) {
            try {
                coverArtDirectory.listFiles()?.toList() ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting cover art files: ${e.message}", e)
                emptyList()
            }
        }
    }
}