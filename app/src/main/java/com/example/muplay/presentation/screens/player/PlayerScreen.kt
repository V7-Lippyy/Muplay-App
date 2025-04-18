package com.example.muplay.presentation.screens.player

import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muplay.presentation.components.LyricsBottomSheet
import com.example.muplay.presentation.components.MetadataEditorDialog
import com.example.muplay.presentation.theme.PlayButtonColor
import com.example.muplay.presentation.viewmodel.LyricsViewModel
import com.example.muplay.util.TimeUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    onBackClick: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentMusic by viewModel.currentMusic.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPosition by viewModel.playbackPosition.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    // Lyrics states
    val lyricsViewModel: LyricsViewModel = hiltViewModel()
    var showLyrics by remember { mutableStateOf(false) }
    val lyrics by lyricsViewModel.synchronizedLyrics.collectAsState()
    val hasLyrics by lyricsViewModel.hasLyrics.collectAsState()
    val isLoadingLyrics by lyricsViewModel.isLoading.collectAsState()
    val lyricsError by lyricsViewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // State for image picker and metadata editor
    var showCustomCoverDialog by remember { mutableStateOf(false) }
    var showMetadataEditorDialog by remember { mutableStateOf(false) }

    // When currentMusic changes, update the LyricsViewModel
    LaunchedEffect(currentMusic) {
        currentMusic?.let { music ->
            lyricsViewModel.init(music)
        }
    }

    // Update playback position in LyricsViewModel when it changes
    LaunchedEffect(playbackPosition) {
        lyricsViewModel.updatePosition(playbackPosition)
    }

    // Content URI dari image picker
    val getContent = rememberImagePickerLauncher { uri ->
        currentMusic?.let { music ->
            coroutineScope.launch {
                // Update custom cover art menggunakan ViewModel
                viewModel.updateCustomCoverArt(music.id, uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sedang Diputar") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        // Add pointer input modifier for detecting swipe up gesture
        modifier = Modifier.pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                // If dragged up significantly, show lyrics
                if (dragAmount < -50 && !showLyrics) {
                    showLyrics = true
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            currentMusic?.let { music ->
                // Album art with edit option
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Album art
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(music.albumArtPath)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Album art",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(MaterialTheme.shapes.large)
                    )

                    // Edit cover button
                    IconButton(
                        onClick = { showCustomCoverDialog = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Ubah Cover",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Song info and favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = music.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = music.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = music.album,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }

                    // Favorite button
                    IconButton(
                        onClick = { viewModel.toggleFavorite() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambahkan ke Favorit",
                            tint = if (isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Edit metadata button
                Spacer(modifier = Modifier.height(8.dp))

                IconButton(
                    onClick = { showMetadataEditorDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Metadata",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Seekbar
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = if (music.duration > 0) playbackPosition.toFloat() / music.duration.toFloat() else 0f,
                        onValueChange = { progress ->
                            val newPosition = (progress * music.duration).toLong()
                            viewModel.seekTo(newPosition)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = TimeUtil.formatDuration(playbackPosition),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = TimeUtil.formatDuration(music.duration),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Playback controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle button
                    IconButton(
                        onClick = { viewModel.toggleShuffleMode() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleMode) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Previous button
                    IconButton(
                        onClick = { viewModel.skipToPrevious() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Play/Pause button
                    FloatingActionButton(
                        onClick = { viewModel.togglePlayPause() },
                        containerColor = PlayButtonColor,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Next button
                    IconButton(
                        onClick = { viewModel.skipToNext() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Repeat button
                    IconButton(
                        onClick = { viewModel.toggleRepeatMode() }
                    ) {
                        val icon = when (repeatMode) {
                            Player.REPEAT_MODE_OFF -> Icons.Default.Repeat
                            Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                            else -> Icons.Default.Repeat
                        }

                        val tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface

                        Icon(
                            imageVector = icon,
                            contentDescription = "Repeat",
                            tint = tint
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Volume control
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeDown,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Slider(
                        value = volume,
                        onValueChange = { viewModel.setVolume(it) },
                        modifier = Modifier.weight(1f)
                    )

                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Show Lyrics button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { showLyrics = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Lyrics")
                    }
                }

                // Custom cover art dialog
                if (showCustomCoverDialog) {
                    AlertDialog(
                        onDismissRequest = { showCustomCoverDialog = false },
                        title = { Text("Ubah Cover Album") },
                        text = { Text("Apakah Anda ingin mengubah cover album untuk lagu ini?") },
                        confirmButton = {
                            TextButton(onClick = {
                                getContent()
                                showCustomCoverDialog = false
                            }) {
                                Text("Pilih Gambar")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomCoverDialog = false }) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // Metadata editor dialog
                if (showMetadataEditorDialog) {
                    MetadataEditorDialog(
                        music = music,
                        onDismiss = { showMetadataEditorDialog = false },
                        onSave = { title, artist, album ->
                            viewModel.updateMusicMetadata(music.id, title, artist, album)
                        }
                    )
                }
            } ?: run {
                // No music loaded state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tidak ada lagu yang diputar",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }

        // Add Lyrics Bottom Sheet
        LyricsBottomSheet(
            isVisible = showLyrics,
            currentMusic = currentMusic,
            lyrics = lyrics,
            hasLyrics = hasLyrics,
            isLoading = isLoadingLyrics,
            errorMessage = lyricsError,
            onDismiss = { showLyrics = false },
            onAddLyrics = { uri -> lyricsViewModel.addLyricsFromUri(uri) },
            onDeleteLyrics = { lyricsViewModel.deleteLyrics() },
            onManualScrollStarted = { lyricsViewModel.onManualScrollStarted() },
            onManualScrollEnded = { lyricsViewModel.onManualScrollEnded() },
            onClearError = { lyricsViewModel.clearError() }
        )
    }
}

@Composable
fun rememberImagePickerLauncher(onImagePicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                onImagePicked(uri)
            }
        }
    )

    return { launcher.launch("image/*") }
}