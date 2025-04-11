package com.example.muplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muplay.presentation.screens.player.PlayerViewModel
import com.example.muplay.presentation.theme.PlayButtonColor
import com.example.muplay.util.TimeUtil

@Composable
fun MusicMiniPlayer(
    onMiniPlayerClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentMusic by viewModel.currentMusic.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()

    // Only show mini player if there's a song loaded
    currentMusic?.let { music ->
        val progress = if (music.duration > 0) {
            playbackPosition.toFloat() / music.duration.toFloat()
        } else {
            0f
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    // Progress bar - Fixed to use proper LinearProgressIndicator parameter
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = PlayButtonColor,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    )

                    // Player content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onMiniPlayerClick)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Album art
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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

                        // Song info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = music.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )

                            Text(
                                text = music.artist,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                            )
                        }

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.End
                        ) {
                            // Previous button
                            IconButton(
                                onClick = { viewModel.skipToPrevious() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipPrevious,
                                    contentDescription = "Previous",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Play/Pause
                            IconButton(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            // Next button
                            IconButton(
                                onClick = { viewModel.skipToNext() },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next",
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}