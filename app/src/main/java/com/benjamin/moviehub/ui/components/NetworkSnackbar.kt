package com.benjamin.moviehub.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import kotlinx.coroutines.delay

@Composable
fun NetworkStatusBar(
    status: ConnectivityStatus,
    modifier: Modifier = Modifier,
) {
    // Composable only visible if we're losing connectivity
    val isOffline = status == ConnectivityStatus.LOST || status == ConnectivityStatus.UNAVAILABLE

    var wasOffline by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (isOffline) {
            wasOffline = true
            showSuccess = false
        } else if (wasOffline && status == ConnectivityStatus.AVAILABLE) {
            showSuccess = true
            wasOffline = false
            delay(5000)
            showSuccess = false
        }
    }

    AnimatedVisibility(
        visible = isOffline,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Snackbar(
            modifier =
                Modifier
                    .padding(16.dp)
                    .padding(bottom = 80.dp)
                    .fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ) {
            Text(
                text = "Pas de connexion internet",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    AnimatedVisibility(
        visible = showSuccess,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Snackbar(
            modifier =
                Modifier
                    .padding(16.dp)
                    .padding(bottom = 80.dp)
                    .fillMaxWidth(),
            containerColor = Color(0xFF4CAF50),
            contentColor = Color.White,
        ) {
            Text(text = "Connexion rétablie", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
