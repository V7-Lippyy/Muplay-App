package com.example.muplay.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muplay.data.model.Music
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
            // Horizontal scrollable list of recently played songs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentlyPlayed) { item ->
                    val music = item.music
                    val playedAt = TimeUtil.getRelativeTimeString(item.playedAt)

                    RecentlyPlayedCard(
                        music = music,
                        playedAt = playedAt,
                        onClick = { onMusicClick(music.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun RecentlyPlayedCard(
    music: Music,
    playedAt: String,
    onClick: () -> Unit
) {
    MusicCard(
        music = music,
        onClick = onClick,
        extraInfo = playedAt,
        isCompact = true
    )
}