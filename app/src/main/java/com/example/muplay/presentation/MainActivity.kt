package com.example.muplay.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muplay.data.repository.MusicRepository
import com.example.muplay.presentation.theme.MuplayTheme
import com.example.muplay.util.NotificationPermissionHelper
import com.example.muplay.util.PermissionHandler
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var notificationPermissionHelper: NotificationPermissionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set layout to cover system UI area
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Initialize permission handler
        permissionHandler = PermissionHandler(this)

        // Initialize notification permission helper for Android 13+
        notificationPermissionHelper = NotificationPermissionHelper(this)
        notificationPermissionHelper.setupPermissionLauncher()

        setContent {
            // Use theme preference from DataStore
            val isDarkTheme by viewModel.darkTheme.collectAsState()
            val hasPermission by permissionHandler.mediaPermissionGranted.collectAsState()

            MuplayTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (hasPermission) {
                        MuplayApp()
                    } else {
                        PermissionRequest(
                            onRequestPermission = { permissionHandler.requestMediaPermission() }
                        )
                    }
                }
            }
        }

        // Request notification permission (only on Android 13+)
        notificationPermissionHelper.checkAndRequestNotificationPermission()
    }

    override fun onResume() {
        super.onResume()
        // Check permission status when app resumes
        permissionHandler.checkMediaPermission()
    }
}

@Composable
fun PermissionRequest(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Izin Diperlukan",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Muplay memerlukan izin untuk mengakses file audio di perangkat Anda untuk menampilkan dan memutar musik.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(onClick = onRequestPermission) {
                Text("Beri Izin")
            }
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    musicRepository: MusicRepository
) : ViewModel() {
    val darkTheme = musicRepository.getDarkThemePreference()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}