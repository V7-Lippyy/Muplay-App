package com.example.muplay.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(navController = navController)
            }
        },
        content = { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                MuplayNavGraph(navController = navController)

                // Mini player hanya ditampilkan jika ada lagu yang diputar dan
                // tidak berada pada layar pemutar penuh
                if (shouldShowBottomBar) {
                    MusicMiniPlayer(
                        onMiniPlayerClick = {
                            navController.navigate(Screen.Player.route)
                        }
                    )
                }
            }
        }
    )
}