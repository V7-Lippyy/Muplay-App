package com.example.muplay.presentation.screens.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.muplay.presentation.components.FavoriteSection
import com.example.muplay.presentation.components.MostPlayedSection
import com.example.muplay.presentation.components.MusicCard
import com.example.muplay.presentation.components.SectionTitle
import com.example.muplay.presentation.screens.player.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "HomeScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMusicClick: (Long) -> Unit,
    onFavoritesClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()

    // Use collectAsStateWithLifecycle for better lifecycle awareness
    val songs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val genres by viewModel.genres.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()

    var isSearchActive by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Log current state of most played songs
    LaunchedEffect(mostPlayedSongs) {
        Log.d(TAG, "Most played songs updated: ${mostPlayedSongs.size} items")
        mostPlayedSongs.forEachIndexed { index, item ->
            Log.d(TAG, "  Item $index: ${item.music.title} by ${item.music.artist}, count: ${item.playCount}")
        }
    }

    // CRITICAL: Force refresh when the screen becomes visible
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    Log.d(TAG, "HomeScreen: ON_CREATE")
                    viewModel.refreshMostPlayedSongs()
                    viewModel.refreshFavoriteSongs()
                }
                Lifecycle.Event.ON_START -> {
                    Log.d(TAG, "HomeScreen: ON_START")
                    viewModel.refreshMostPlayedSongs()
                    viewModel.refreshFavoriteSongs()
                }
                Lifecycle.Event.ON_RESUME -> {
                    Log.d(TAG, "HomeScreen: ON_RESUME")
                    viewModel.refreshMostPlayedSongs()
                    viewModel.refreshFavoriteSongs()

                    // Extra refresh after a short delay to ensure data loads
                    coroutineScope.launch {
                        delay(300)
                        viewModel.refreshMostPlayedSongs()
                        viewModel.refreshFavoriteSongs()
                    }
                }
                else -> { /* ignore other events */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Force initial refresh of most played songs and favorites
    LaunchedEffect(Unit) {
        Log.d(TAG, "Initial launch effect")
        viewModel.refreshMostPlayedSongs()
        viewModel.refreshFavoriteSongs()

        // Double refresh with delay for better reliability
        delay(300)
        viewModel.refreshMostPlayedSongs()
        viewModel.refreshFavoriteSongs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beranda") },
                actions = {
                    // Refresh button
                    IconButton(onClick = {
                        viewModel.refreshMostPlayedSongs()
                        viewModel.refreshFavoriteSongs()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Menyegarkan data lagu")
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data"
                        )
                    }

                    IconButton(onClick = { isSearchActive = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Cari"
                        )
                    }

                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search bar
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    onSearch = { isSearchActive = false },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("Cari lagu, artis, atau album") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Cari")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search results shown here when active
                    LazyColumn {
                        items(songs) { song ->
                            MusicCard(
                                music = song,
                                onClick = {
                                    Log.d(TAG, "Music clicked: ${song.id}, title: ${song.title}")
                                    try {
                                        // Play music first, then navigate
                                        playerViewModel.playMusic(song.id)
                                        onMusicClick(song.id)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error playing music: ${e.message}", e)
                                    }
                                    isSearchActive = false
                                }
                            )
                        }
                    }
                }
            }

            // Filters
            if (showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Filter Artis",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedArtist == null,
                                onClick = { viewModel.selectArtist(null) },
                                label = { Text("Semua") }
                            )
                            Spacer(modifier = Modifier.padding(end = 8.dp))
                        }
                        items(artists) { artist ->
                            FilterChip(
                                selected = selectedArtist == artist,
                                onClick = { viewModel.selectArtist(artist) },
                                label = { Text(artist) }
                            )
                            Spacer(modifier = Modifier.padding(end = 8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Filter Genre",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedGenre == null,
                                onClick = { viewModel.selectGenre(null) },
                                label = { Text("Semua") }
                            )
                            Spacer(modifier = Modifier.padding(end = 8.dp))
                        }
                        items(genres) { genre ->
                            FilterChip(
                                selected = selectedGenre == genre,
                                onClick = { viewModel.selectGenre(genre) },
                                label = { Text(genre) }
                            )
                            Spacer(modifier = Modifier.padding(end = 8.dp))
                        }
                    }
                }
            }

            // Main content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Only show special sections if we're not filtering or searching
                if (searchQuery.isEmpty() && selectedArtist == null && selectedGenre == null) {
                    // Favorites Section
                    item {
                        FavoriteSection(
                            favoriteSongs = favoriteSongs,
                            onMusicClick = { musicId: Long ->
                                try {
                                    playerViewModel.playMusic(musicId)
                                    onMusicClick(musicId)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error playing music: ${e.message}", e)
                                }
                            },
                            onViewAllClick = onFavoritesClick
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // Most Played Section - ALWAYS show this section, even if empty
                    item {
                        MostPlayedSection(
                            mostPlayedSongs = mostPlayedSongs,
                            onMusicClick = { musicId: Long ->
                                try {
                                    playerViewModel.playMusic(musicId)
                                    onMusicClick(musicId)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error playing music: ${e.message}", e)
                                }
                            }
                        )
                        // Log the rendering of MostPlayedSection
                        LaunchedEffect(mostPlayedSongs) {
                            Log.d(TAG, "Rendering MostPlayedSection with ${mostPlayedSongs.size} items")
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Section title for all songs
                item {
                    SectionTitle(
                        title = if (searchQuery.isNotEmpty() || selectedArtist != null || selectedGenre != null) {
                            "Hasil Pencarian"
                        } else {
                            "Semua Lagu"
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // All songs or filtered songs
                items(songs) { song ->
                    MusicCard(
                        music = song,
                        onClick = {
                            Log.d(TAG, "Music clicked from list: ${song.id}, title: ${song.title}")
                            try {
                                // Play music first, then navigate
                                playerViewModel.playMusic(song.id)
                                onMusicClick(song.id)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error playing music from list: ${e.message}", e)
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // Show empty state if no songs
                if (songs.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada lagu ditemukan",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp, horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}