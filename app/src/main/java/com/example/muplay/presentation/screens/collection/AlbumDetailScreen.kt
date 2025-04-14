package com.example.muplay.presentation.screens.collection

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muplay.presentation.components.MusicCard
import com.example.muplay.presentation.screens.player.PlayerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumName: String,
    onBackClick: () -> Unit,
    onMusicClick: (Long) -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val album by viewModel.albumDetails.collectAsState()
    val songs by viewModel.albumSongs.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Initialize view model with the album name
    viewModel.initWithAlbumName(albumName)

    var showCoverPickerDialog by remember { mutableStateOf(false) }

    // Image picker launcher
    val coverPickerLauncher = rememberImagePickerLauncher { uri ->
        coroutineScope.launch {
            viewModel.updateAlbumCover(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(albumName) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (songs.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (songs.isNotEmpty()) {
                            playerViewModel.playMusic(songs.first().id)
                        }
                    },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Play All") }
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp)
        ) {
            // Album header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album cover with edit option
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        album?.coverArtPath?.let { path ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(path)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Album cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: Icon(
                            imageVector = Icons.Default.Album,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        // Edit button overlay
                        IconButton(
                            onClick = { showCoverPickerDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                    androidx.compose.foundation.shape.CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Cover",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    album?.artist?.let { artist ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${songs.size} songs",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Songs in the album
            if (songs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No songs in this album",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                items(songs) { song ->
                    // Pastikan menggunakan MusicCard dengan parameter yang benar
                    MusicCard(
                        music = song,
                        onClick = { onMusicClick(song.id) },
                        isCompact = false // Gunakan tampilan regular, bukan compact
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Cover art picker dialog
        if (showCoverPickerDialog) {
            AlertDialog(
                onDismissRequest = { showCoverPickerDialog = false },
                title = { Text("Update Album Cover") },
                text = { Text("Would you like to change the cover art for this album?") },
                confirmButton = {
                    TextButton(onClick = {
                        coverPickerLauncher()
                        showCoverPickerDialog = false
                    }) {
                        Text("Choose Image")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCoverPickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
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