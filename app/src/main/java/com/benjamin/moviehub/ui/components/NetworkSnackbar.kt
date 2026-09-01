package com.benjamin.moviehub.ui.components

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus

@Composable
fun NetworkStatusBar(
    status: ConnectivityStatus,
    snackbarHostState: SnackbarHostState,
) {
    val isOffline = status == ConnectivityStatus.LOST || status == ConnectivityStatus.UNAVAILABLE
    val offlineMessage = stringResource(R.string.no_internet_connection)
    val restoredMessage = stringResource(R.string.connection_restored)

    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (isOffline) {
            wasOffline = true
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = offlineMessage,
                duration = SnackbarDuration.Indefinite,
            )
        } else if (wasOffline && status == ConnectivityStatus.AVAILABLE) {
            wasOffline = false
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = restoredMessage,
                duration = SnackbarDuration.Long,
            )
        }
    }
}

@Composable
fun NetworkStatusBar(snackbarData: SnackbarData) {
    val isOffline = snackbarData.visuals.message == stringResource(R.string.no_internet_connection)
    Snackbar(
        containerColor = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
        contentColor =
            if (isOffline) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(text = snackbarData.visuals.message, style = MaterialTheme.typography.bodyMedium)
    }
}
