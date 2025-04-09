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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muplay.R
import com.example.muplay.data.model.Music
import com.example.muplay.presentation.components.HighlightCard
import com.example.muplay.presentation.components.MusicCard
import com.example.muplay.presentation.components.RecentlyPlayedCard
import com.example.muplay.presentation.components.SectionTitle
import com.example.muplay.presentation.components.TotalSongsCard
import com.example.muplay.presentation.screens.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMusicClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedGenre by viewModel.selectedGenre.collectAsState()
    val songs by viewModel.filteredSongs.collectAsState()
    val artists by viewModel.artists.collectAsState()
    val genres by viewModel.genres.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val mostPlayed by viewModel.mostPlayed.collectAsState()
    val allSongs by viewModel.totalSongs.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beranda") },
                actions = {
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
        }
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
                                    Log.d("HomeScreen", "Music clicked: ${song.id}, title: ${song.title}")
                                    try {
                                        // Play music first, then navigate
                                        playerViewModel.playMusic(song.id)
                                        onMusicClick(song.id)
                                    } catch (e: Exception) {
                                        Log.e("HomeScreen", "Error playing music: ${e.message}", e)
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
                contentPadding = PaddingValues(16.dp)
            ) {
                // Highlight cards
                item {
                    LazyRow {
                        // Only show highlight cards if we're not filtering or searching
                        if (searchQuery.isEmpty() && selectedArtist == null && selectedGenre == null) {
                            // Last played song
                            if (recentlyPlayed.isNotEmpty()) {
                                item {
                                    val lastPlayedMusic = viewModel.getMusicFromHistory(recentlyPlayed.first())
                                    RecentlyPlayedCard(
                                        music = lastPlayedMusic,
                                        onClick = {
                                            try {
                                                // Play music first, then navigate
                                                playerViewModel.playMusic(lastPlayedMusic.id)
                                                onMusicClick(lastPlayedMusic.id)
                                            } catch (e: Exception) {
                                                Log.e("HomeScreen", "Error playing recent music: ${e.message}", e)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.padding(end = 16.dp))
                                }
                            }

                            // Total songs card
                            item {
                                TotalSongsCard(songCount = allSongs.size)
                                Spacer(modifier = Modifier.padding(end = 16.dp))
                            }

                            // Most played song if available
                            if (mostPlayed.isNotEmpty()) {
                                item {
                                    val mostPlayedMusic = viewModel.getMusicFromHistory(mostPlayed.first())
                                    HighlightCard(
                                        title = "Lagu Populer",
                                        icon = Icons.Default.Favorite,
                                        music = mostPlayedMusic,
                                        onClick = {
                                            try {
                                                // Play music first, then navigate
                                                playerViewModel.playMusic(mostPlayedMusic.id)
                                                onMusicClick(mostPlayedMusic.id)
                                            } catch (e: Exception) {
                                                Log.e("HomeScreen", "Error playing popular music: ${e.message}", e)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
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
                            Log.d("HomeScreen", "Music clicked from list: ${song.id}, title: ${song.title}")
                            try {
                                // Play music first, then navigate
                                playerViewModel.playMusic(song.id)
                                onMusicClick(song.id)
                            } catch (e: Exception) {
                                Log.e("HomeScreen", "Error playing music from list: ${e.message}", e)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show empty state if no songs
                if (songs.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada lagu ditemukan",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp)
                        )
                    }
                }
            }
        }
    }
}