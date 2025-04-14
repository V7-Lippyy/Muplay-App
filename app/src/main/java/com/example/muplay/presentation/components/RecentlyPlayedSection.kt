package com.example.muplay.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muplay.data.model.MusicWithHistory
import com.example.muplay.util.TimeUtil

@Composable
fun RecentlyPlayedSection(
    recentlyPlayed: List<MusicWithHistory>,
    onMusicClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section title
        Text(
            text = "Terakhir Diputar",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Description text
        Text(
            text = "Lagu yang baru saja Anda dengarkan",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (recentlyPlayed.isEmpty()) {
            Text(
                text = "Belum ada lagu yang diputar",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            // Custom 2x3 grid using Column and Row
            val items = recentlyPlayed.take(6) // Limit to 6 items for 2x3 grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row (items 0, 1, 2)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (index in 0 until 3) {
                        if (index < items.size) {
                            val musicWithHistory = items[index]
                            val music = musicWithHistory.music
                            val playedAt = TimeUtil.getRelativeTimeString(musicWithHistory.playedAt)
                            CompactMusicCard(
                                music = music,
                                onClick = { onMusicClick(music.id) },
                                extraInfo = playedAt,
                                modifier = Modifier
                                    .weight(1f)
                                    .width(120.dp)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Second row (items 3, 4, 5)
                if (items.size > 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (index in 3 until 6) {
                            if (index < items.size) {
                                val musicWithHistory = items[index]
                                val music = musicWithHistory.music
                                val playedAt = TimeUtil.getRelativeTimeString(musicWithHistory.playedAt)
                                CompactMusicCard(
                                    music = music,
                                    onClick = { onMusicClick(music.id) },
                                    extraInfo = playedAt,
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(120.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}