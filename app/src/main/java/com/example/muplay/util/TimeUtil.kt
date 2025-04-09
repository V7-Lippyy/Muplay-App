package com.example.muplay.util

import java.util.concurrent.TimeUnit

object TimeUtil {

    /**
     * Format durasi dari milidetik ke format "mm:ss"
     */
    fun formatDuration(durationMs: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                TimeUnit.MINUTES.toSeconds(minutes)

        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Format durasi dari milidetik ke format "hh:mm:ss" jika lebih dari 1 jam
     */
    fun formatDurationLong(durationMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) -
                TimeUnit.HOURS.toMinutes(hours)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMs))

        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format tanggal kembali ke string yang menunjukkan waktu relatif, seperti
     * "Baru saja", "5 menit yang lalu", "2 jam yang lalu", "Kemarin", dsb.
     */
    fun getRelativeTimeString(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            seconds < 60 -> "Baru saja"
            minutes < 60 -> "$minutes menit yang lalu"
            hours < 24 -> "$hours jam yang lalu"
            days < 2 -> "Kemarin"
            days < 7 -> "$days hari yang lalu"
            days < 30 -> "${days / 7} minggu yang lalu"
            days < 365 -> "${days / 30} bulan yang lalu"
            else -> "${days / 365} tahun yang lalu"
        }
    }
}