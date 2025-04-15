package com.example.muplay.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.muplay.presentation.screens.collection.AlbumDetailScreen
import com.example.muplay.presentation.screens.collection.ArtistDetailScreen
import com.example.muplay.presentation.screens.collection.CollectionScreen
import com.example.muplay.presentation.screens.home.HomeScreen
import com.example.muplay.presentation.screens.player.PlayerScreen
import com.example.muplay.presentation.screens.player.PlayerViewModel
import com.example.muplay.presentation.screens.playlist.PlaylistDetailScreen
import com.example.muplay.presentation.screens.splash.SplashScreen

@Composable
fun MuplayNavGraph(
    navController: NavHostController
) {
    // Share PlayerViewModel across screens
    val playerViewModel: PlayerViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Navigating to player with musicId: $musicId")
                    // Already handled playing in HomeScreen
                    navController.navigate(Screen.Player.route)
                },
                onFavoritesClick = {
                    // Navigate to Collection screen with Favorites tab selected
                    navController.navigate(Screen.Collection.route) {
                        // You can pass the selected tab index as a parameter if needed
                        // Or handle this within the Collection screen
                        popUpTo(Screen.Home.route) { saveState = true }
                    }
                },
                playerViewModel = playerViewModel
            )
        }

        // Player Screen
        composable(Screen.Player.route) {
            PlayerScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                viewModel = playerViewModel
            )
        }

        // Collection Screen
        composable(Screen.Collection.route) {
            CollectionScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                },
                onAlbumClick = { albumName ->
                    navController.navigate(Screen.AlbumDetail.createRoute(albumName))
                },
                onArtistClick = { artistName ->
                    navController.navigate(Screen.ArtistDetail.createRoute(artistName))
                },
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Playing from collection, musicId: $musicId")
                    playerViewModel.playMusic(musicId)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        // Playlist Detail Screen
        composable(
            route = Screen.PlaylistDetail.route,
            arguments = listOf(
                navArgument("playlistId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: -1L
            PlaylistDetailScreen(
                playlistId = playlistId,
                onBackClick = {
                    navController.popBackStack()
                },
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Playing from playlist, musicId: $musicId")
                    playerViewModel.playMusic(musicId)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        // Album Detail Screen
        composable(
            route = Screen.AlbumDetail.route,
            arguments = listOf(
                navArgument("albumName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val albumName = backStackEntry.arguments?.getString("albumName") ?: ""
            AlbumDetailScreen(
                albumName = albumName,
                onBackClick = {
                    navController.popBackStack()
                },
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Playing from album, musicId: $musicId")
                    playerViewModel.playMusic(musicId)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        // Artist Detail Screen
        composable(
            route = Screen.ArtistDetail.route,
            arguments = listOf(
                navArgument("artistName") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val artistName = backStackEntry.arguments?.getString("artistName") ?: ""
            ArtistDetailScreen(
                artistName = artistName,
                onBackClick = {
                    navController.popBackStack()
                },
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Playing from artist, musicId: $musicId")
                    playerViewModel.playMusic(musicId)
                    navController.navigate(Screen.Player.route)
                }
            )
        }
    }
}