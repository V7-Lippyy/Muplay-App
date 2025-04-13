package com.example.muplay.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.muplay.R
import com.example.muplay.presentation.navigation.Screen

data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val screen: Screen
)

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem(
            title = "Beranda",
            icon = Icons.Default.Home,
            screen = Screen.Home
        ),
        BottomNavItem(
            title = "Koleksi",
            icon = Icons.Default.LibraryMusic, // Changed icon to be more appropriate for collection
            screen = Screen.Collection // We'll rename this in the Screen class
        )
    )

    // Get current route
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route

            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selected,
                onClick = {
                    if (currentRoute != item.screen.route) {
                        navController.navigate(item.screen.route) {
                            // Avoid multiple stack entries for the same destination
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            // Launchmode single top
                            launchSingleTop = true
                            // Restore state when navigating back to previous position
                            restoreState = true
                        }
                    }
                }
            )
        }
    }
}