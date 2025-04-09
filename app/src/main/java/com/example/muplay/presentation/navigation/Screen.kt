package com.example.muplay.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Player : Screen("player")
    object History : Screen("history")
    object Playlist : Screen("playlist")

    // Route dengan argumen
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long): String = "playlist/$playlistId"
    }
}