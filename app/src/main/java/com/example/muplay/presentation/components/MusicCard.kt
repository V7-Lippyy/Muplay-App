package com.example.muplay.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muplay.data.model.Music
import com.example.muplay.util.TimeUtil

/**
 * A music card component that can be displayed in either regular or compact layout.
 * This version is backward compatible with existing code that only passes music and onClick.
 */
@Composable
fun MusicCard(
    music: Music,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extraInfo: String? = null,
    isCompact: Boolean = false
) {
    if (isCompact) {
        CompactMusicCard(music, onClick, extraInfo, modifier)
    } else {
        RegularMusicCard(music, onClick, extraInfo, modifier)
    }
}

/**
 * A card that displays a music item in a regular row format (album art on left, details on right)
 */
@Composable
fun RegularMusicCard(
    music: Music,
    onClick: () -> Unit,
    extraInfo: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.small)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(music.albumArtPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Song Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = music.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Optional extra info (like "Played 5 minutes ago")
                if (extraInfo != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Duration
            Text(
                text = TimeUtil.formatDuration(music.duration),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * A card that displays a music item in a compact card format, suitable for grids and special sections.
 */
@Composable
fun CompactMusicCard(
    music: Music,
    onClick: () -> Unit,
    extraInfo: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(180.dp) // Fixed height to ensure consistent grid layout
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Album Art (square aspect ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Make it square
                    .clip(MaterialTheme.shapes.medium)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(music.albumArtPath)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Song Details
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = music.title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = music.artist,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                // Extra info (like "Played 5 minutes ago")
                if (extraInfo != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}