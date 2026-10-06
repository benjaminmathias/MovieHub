package com.benjamin.moviehub.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.connectivity.ConnectivityStatus
import com.benjamin.moviehub.domain.connectivity.isOffline

@Composable
fun NetworkStatusEffect(
    status: ConnectivityStatus,
    snackbarHostState: SnackbarHostState,
) {
    val isOffline = status.isOffline
    val offlineMessage = stringResource(R.string.no_internet_connection)
    val restoredMessage = stringResource(R.string.connection_restored)

    var wasOffline by rememberSaveable { mutableStateOf(false) }

    val isAvailable = status == ConnectivityStatus.AVAILABLE

    // Keyed on the offline flag rather than the raw status so LOST <-> UNAVAILABLE does not
    // re-announce. Restoration is announced only once a usable connection is back (AVAILABLE);
    // the offline banner queues behind unrelated messages instead of dismissing them.
    LaunchedEffect(isOffline, isAvailable) {
        if (isOffline) {
            wasOffline = true
            snackbarHostState.showSnackbar(
                message = offlineMessage,
                duration = SnackbarDuration.Short,
            )
        } else if (wasOffline && isAvailable) {
            wasOffline = false
            snackbarHostState.showSnackbar(
                message = restoredMessage,
                duration = SnackbarDuration.Long,
            )
        }
    }
}

@Composable
fun NetworkSnackbar(
    snackbarData: SnackbarData,
    isOffline: Boolean,
) {
    Snackbar(
        containerColor = if (isOffline) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primaryContainer,
        contentColor =
            if (isOffline) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Text(text = snackbarData.visuals.message, style = MaterialTheme.typography.bodyMedium)
    }
}
