package com.example.muplay.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Utility class for image picking and saving operations
 */
object ImagePickerUtil {
    private const val TAG = "ImagePickerUtil"

    /**
     * Composable function to launch an image picker
     */
    @Composable
    fun rememberImagePicker(
        onImagePicked: (Uri) -> Unit
    ): () -> Unit {
        val context = LocalContext.current

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let { onImagePicked(it) }
        }

        return { launcher.launch("image/*") }
    }

    /**
     * Save an image to app's internal storage
     *
     * @param context Application context
     * @param uri Source URI of the image
     * @param fileName Custom file name for the saved image
     * @return Path to the saved image or null if failed
     */
    suspend fun saveImageToInternalStorage(
        context: Context,
        uri: Uri,
        fileName: String = "cover_${UUID.randomUUID()}.jpg"
    ): String? {
        return try {
            withContext(Dispatchers.IO) {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = File(context.filesDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    inputStream.close()
                    file.absolutePath
                } else {
                    Log.e(TAG, "Failed to open input stream for URI: $uri")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving image: ${e.message}", e)
            null
        }
    }

    /**
     * Get a content URI for a file in the app's internal storage
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Load an image from a URI and convert to Bitmap
     */
    suspend fun loadImageFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val drawable = withContext(Dispatchers.IO) {
                loader.execute(request).drawable
            }

            (drawable as? BitmapDrawable)?.bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error loading image: ${e.message}", e)
            null
        }
    }
}