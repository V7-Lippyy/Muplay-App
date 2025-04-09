package com.example.muplay.util

import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Kelas untuk penanganan error dan logging
 */
object ErrorHandling {
    private const val TAG = "Muplay"

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val exception: Throwable) : Result<Nothing>()
    }

    /**
     * Jalankan fungsi suspend dengan penanganan error
     */
    suspend fun <T> runCatching(block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (e: CancellationException) {
            // Re-throw cancellation exceptions
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message}", e)
            Result.Error(e)
        }
    }

    /**
     * Extension function untuk menangani error pada Flow
     */
    fun <T> Flow<T>.handleErrors(tag: String): Flow<T> {
        return this.catch { e ->
            if (e is CancellationException) {
                throw e
            }
            Log.e(TAG, "Flow error in $tag: ${e.message}", e)
            // Kita rethrow error untuk dapat ditangani di level UI
            throw e
        }
    }

    /**
     * Logging ke Logcat dengan tag Muplay
     */
    fun logError(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }

    fun logWarning(message: String) {
        Log.w(TAG, message)
    }

    fun logInfo(message: String) {
        Log.i(TAG, message)
    }

    fun logDebug(message: String) {
        Log.d(TAG, message)
    }
}