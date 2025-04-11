package com.example.muplay.presentation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Player : Screen("player")
    object History : Screen("history")

    // Change Playlist to Collection
    object Collection : Screen("collection")

    // Route with arguments
    object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: Long): String = "playlist/$playlistId"
    }

    // New routes for Album and Artist details
    object AlbumDetail : Screen("album/{albumName}") {
        fun createRoute(albumName: String): String = "album/$albumName"
    }

    object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String): String = "artist/$artistName"
    }
}