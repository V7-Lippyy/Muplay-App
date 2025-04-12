package com.example.muplay.presentation.screens.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muplay.data.model.Playlist
import com.example.muplay.presentation.components.HistoryMusicCard
import com.example.muplay.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onMusicClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val historyItems by viewModel.historyList.collectAsState()
    val sortType by viewModel.sortType.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val isCleaningUp by viewModel.isCleaningUp.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }
    var currentlySelectedMusic by remember { mutableStateOf<Long?>(null) }
    var showPlaylistMenu by remember { mutableStateOf(false) }

    // Snackbar untuk pesan status
    val snackbarHostState = remember { SnackbarHostState() }

    // Tampilkan pesan status di snackbar jika ada
    LaunchedEffect(statusMessage) {
        if (statusMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(statusMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Musik") },
                actions = {
                    // Sort button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Urutkan")
                    }

                    // More menu
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu Lainnya")
                    }

                    // More dropdown menu
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Bersihkan Duplikasi") },
                            leadingIcon = {
                                Icon(Icons.Default.FilterAlt, contentDescription = null)
                            },
                            onClick = {
                                showCleanupDialog = true
                                showMoreMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Hapus Semua Riwayat") },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                            onClick = {
                                showClearDialog = true
                                showMoreMenu = false
                            }
                        )
                    }

                    // Sort dropdown menu
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Terbaru Dulu") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.TIME_DESC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.TIME_DESC)
                                showSortMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Terlama Dulu") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.TIME_ASC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.TIME_ASC)
                                showSortMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Judul (A-Z)") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.TITLE_ASC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.TITLE_ASC)
                                showSortMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Judul (Z-A)") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.TITLE_DESC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.TITLE_DESC)
                                showSortMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Artis (A-Z)") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.ARTIST_ASC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.ARTIST_ASC)
                                showSortMenu = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Artis (Z-A)") },
                            leadingIcon = {
                                RadioButton(
                                    selected = sortType == HistoryViewModel.SortType.ARTIST_DESC,
                                    onClick = null
                                )
                            },
                            onClick = {
                                viewModel.setSortType(HistoryViewModel.SortType.ARTIST_DESC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            if (historyItems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Text(
                        text = "Belum Ada Riwayat",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Putar beberapa lagu untuk melihat riwayat di sini",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // List of history items
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(historyItems) { item ->
                        val music = viewModel.getMusicFromHistory(item)
                        HistoryMusicCard(
                            music = music,
                            playedAt = TimeUtil.getRelativeTimeString(item.playedAt),
                            onClick = { onMusicClick(music.id) },
                            onAddToPlaylist = {
                                currentlySelectedMusic = music.id
                                showPlaylistMenu = true
                            },
                            onDelete = { viewModel.deleteHistoryEntry(item.historyId) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            // Add to playlist menu
            if (showPlaylistMenu && currentlySelectedMusic != null) {
                AlertDialog(
                    onDismissRequest = { showPlaylistMenu = false },
                    title = { Text("Tambahkan ke Playlist") },
                    text = {
                        Column {
                            if (playlists.isEmpty()) {
                                Text("Tidak ada playlist tersedia. Buat playlist terlebih dahulu.")
                            } else {
                                LazyColumn {
                                    items(playlists) { playlist ->
                                        DropdownMenuItem(
                                            text = { Text(playlist.name) },
                                            onClick = {
                                                currentlySelectedMusic?.let { musicId ->
                                                    viewModel.addToPlaylist(playlist.id, musicId)
                                                }
                                                showPlaylistMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showPlaylistMenu = false }
                        ) {
                            Text("Tutup")
                        }
                    }
                )
            }

            // Clear history dialog
            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Hapus Semua Riwayat") },
                    text = { Text("Anda yakin ingin menghapus semua riwayat pemutaran musik?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearAllHistory()
                                showClearDialog = false
                            }
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showClearDialog = false }
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }

            // Cleanup duplicates dialog
            if (showCleanupDialog) {
                AlertDialog(
                    onDismissRequest = { showCleanupDialog = false },
                    title = { Text("Bersihkan Duplikasi") },
                    text = { Text("Anda ingin membersihkan entri duplikat dalam riwayat? Ini akan menghapus entri duplikat yang berasal dari lagu yang sama dalam satu hari.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.cleanupDuplicateHistory()
                                showCleanupDialog = false
                            },
                            enabled = !isCleaningUp
                        ) {
                            Text("Bersihkan")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showCleanupDialog = false }
                        ) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}