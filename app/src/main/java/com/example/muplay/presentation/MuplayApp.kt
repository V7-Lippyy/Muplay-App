package com.example.muplay.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.muplay.presentation.components.BottomNavigationBar
import com.example.muplay.presentation.components.MusicMiniPlayer
import com.example.muplay.presentation.navigation.MuplayNavGraph
import com.example.muplay.presentation.navigation.Screen

@Composable
fun MuplayApp() {
    val navController = rememberNavController()

    // Ambil rute saat ini
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Tentukan apakah mini player dan bottom navigation harus ditampilkan
    val shouldShowBottomBar = currentRoute != Screen.Splash.route &&
            currentRoute != Screen.Player.route

    val shouldShowMiniPlayer = shouldShowBottomBar && currentRoute != null

    Scaffold(
        bottomBar = {
            // BottomBar and MiniPlayer stacked together
            if (shouldShowBottomBar) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Mini player on top of bottom navigation
                    if (shouldShowMiniPlayer) {
                        MusicMiniPlayer(
                            onMiniPlayerClick = {
                                navController.navigate(Screen.Player.route)
                            }
                        )
                    }

                    // Bottom navigation below mini player
                    BottomNavigationBar(navController = navController)
                }
            }
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                MuplayNavGraph(navController = navController)
            }
        }
    )
}