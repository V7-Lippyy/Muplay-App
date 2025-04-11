package com.example.muplay.presentation.screens.collection.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muplay.presentation.components.PlaylistCard
import com.example.muplay.presentation.screens.collection.CollectionViewModel

@Composable
fun PlaylistsTab(
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    showCreateDialog: Boolean,
    onDismissCreateDialog: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    val playlists by viewModel.allPlaylists.collectAsState()
    var newPlaylistName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (playlists.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Text(
                    text = "Belum Ada Playlist",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Buat playlist untuk mengorganisir lagu favoritmu",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        newPlaylistName = ""
                        onDismissCreateDialog()
                    }
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Buat Playlist")
                }
            }
        } else {
            // Grid of playlists
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(playlists) { playlistWithMusic ->
                    PlaylistCard(
                        playlist = playlistWithMusic.playlist,
                        songCount = playlistWithMusic.songs.size,
                        onClick = { onPlaylistClick(playlistWithMusic.playlist.id) }
                    )
                }
            }
        }

        // Create playlist dialog
        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = onDismissCreateDialog,
                title = { Text("Buat Playlist Baru") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Nama Playlist") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName)
                                onDismissCreateDialog()
                            }
                        }
                    ) {
                        Text("Buat")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDismissCreateDialog
                    ) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}