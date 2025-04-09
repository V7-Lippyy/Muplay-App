package com.example.muplay.presentation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.muplay.presentation.screens.history.HistoryScreen
import com.example.muplay.presentation.screens.home.HomeScreen
import com.example.muplay.presentation.screens.player.PlayerScreen
import com.example.muplay.presentation.screens.player.PlayerViewModel
import com.example.muplay.presentation.screens.playlist.PlaylistDetailScreen
import com.example.muplay.presentation.screens.playlist.PlaylistScreen
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

        // History Screen
        composable(Screen.History.route) {
            HistoryScreen(
                onMusicClick = { musicId ->
                    Log.d("Navigation", "Playing from history, musicId: $musicId")
                    playerViewModel.playMusic(musicId)
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        // Playlist Screen
        composable(Screen.Playlist.route) {
            PlaylistScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
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
    }
}