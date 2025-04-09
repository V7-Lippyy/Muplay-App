package com.example.muplay.presentation.screens.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.muplay.data.model.Music
import com.example.muplay.presentation.components.MusicCard
import com.example.muplay.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    onMusicClick: (Long) -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val playlistWithMusic by viewModel.playlistWithMusic.collectAsState()
    val songs by viewModel.songs.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    playlistWithMusic?.playlist?.let { playlist ->
        // Inisialisasi nama playlist untuk dialog rename
        if (newPlaylistName.isEmpty()) {
            newPlaylistName = playlist.name
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(playlist.name) },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ubah Nama") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    newPlaylistName = playlist.name
                                    showRenameDialog = true
                                    showMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("Hapus Playlist") },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                },
                                onClick = {
                                    showDeleteDialog = true
                                    showMenu = false
                                }
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                if (songs.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { /* TODO: Putar semua lagu dalam playlist */ },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text("Putar Semua") }
                    )
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showAddSongsDialog = true },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Tambah Lagu") }
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (songs.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        Text(
                            text = "Playlist Kosong",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Tambahkan lagu ke playlist ini",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Playlist header and songs
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        // Playlist header
                        item {
                            PlaylistHeader(
                                playlistName = playlist.name,
                                coverArtPath = playlist.coverArtPath,
                                songsCount = songs.size,
                                totalDuration = calculateTotalDuration(songs)
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Add song button
                        item {
                            TextButton(
                                onClick = { showAddSongsDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text("Tambah Lagu")
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Songs list
                        items(songs) { song ->
                            PlaylistMusicCard(
                                music = song,
                                onClick = { onMusicClick(song.id) },
                                onRemove = { viewModel.removeSongFromPlaylist(song.id) }
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // Rename dialog
                if (showRenameDialog) {
                    AlertDialog(
                        onDismissRequest = { showRenameDialog = false },
                        title = { Text("Ubah Nama Playlist") },
                        text = {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Nama Playlist") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        viewModel.renamePlaylist(newPlaylistName)
                                    }
                                    showRenameDialog = false
                                }
                            ) {
                                Text("Simpan")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showRenameDialog = false }
                            ) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // Delete confirmation dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Hapus Playlist") },
                        text = { Text("Apakah Anda yakin ingin menghapus playlist ini?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.deletePlaylist()
                                    onBackClick()
                                }
                            ) {
                                Text("Hapus")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDeleteDialog = false }
                            ) {
                                Text("Batal")
                            }
                        }
                    )
                }

                // Add songs dialog
                if (showAddSongsDialog) {
                    val availableSongs = viewModel.getSongsNotInPlaylist()

                    AlertDialog(
                        onDismissRequest = { showAddSongsDialog = false },
                        title = { Text("Tambah Lagu ke Playlist") },
                        text = {
                            if (availableSongs.isEmpty()) {
                                Text("Semua lagu sudah ada dalam playlist ini.")
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(availableSongs) { song ->
                                        MusicCard(
                                            music = song,
                                            onClick = {
                                                viewModel.addSongToPlaylist(song.id)
                                                showAddSongsDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { showAddSongsDialog = false }
                            ) {
                                Text("Tutup")
                            }
                        }
                    )
                }
            }
        }
    } ?: run {
        // Loading or error state
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Memuat playlist...")
        }
    }
}

@Composable
fun PlaylistHeader(
    playlistName: String,
    coverArtPath: String?,
    songsCount: Int,
    totalDuration: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cover art
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(MaterialTheme.shapes.medium)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(coverArtPath)
                    .crossfade(true)
                    .build(),
                contentDescription = "Playlist cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Playlist info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlistName,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$songsCount lagu • ${TimeUtil.formatDurationLong(totalDuration)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PlaylistMusicCard(
    music: Music,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MusicCard(
            music = music,
            onClick = onClick,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus dari playlist"
            )
        }
    }
}

// Fungsi untuk menghitung total durasi lagu dalam playlist
private fun calculateTotalDuration(songs: List<Music>): Long {
    return songs.sumOf { it.duration }
}