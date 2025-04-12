package com.example.muplay.presentation.screens.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muplay.presentation.screens.collection.tabs.AlbumsTab
import com.example.muplay.presentation.screens.collection.tabs.ArtistsTab
import com.example.muplay.presentation.screens.collection.tabs.PlaylistsTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onPlaylistClick: (Long) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    viewModel: CollectionViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // UI states
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val refreshMessage by viewModel.refreshMessage.collectAsState()

    // Snackbar untuk refresh status
    val snackbarHostState = remember { SnackbarHostState() }

    // Show snackbar when refresh message changes
    LaunchedEffect(refreshMessage) {
        refreshMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Koleksi") },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.forceRefreshCollections() },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Koleksi",
                            tint = if (isRefreshing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            // Only show FAB in Playlists tab
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = {
                        newPlaylistName = ""
                        showCreatePlaylistDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Playlist")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = selectedTabIndex
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Playlist",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Album",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "Artis",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }

            // Tab content
            when (selectedTabIndex) {
                0 -> PlaylistsTab(
                    onPlaylistClick = onPlaylistClick,
                    onCreatePlaylist = { name ->
                        viewModel.createPlaylist(name)
                    },
                    showCreateDialog = showCreatePlaylistDialog,
                    onDismissCreateDialog = { showCreatePlaylistDialog = false }
                )
                1 -> AlbumsTab(
                    onAlbumClick = onAlbumClick,
                    isRefreshing = isRefreshing
                )
                2 -> ArtistsTab(
                    onArtistClick = onArtistClick,
                    isRefreshing = isRefreshing
                )
            }
        }
    }
}