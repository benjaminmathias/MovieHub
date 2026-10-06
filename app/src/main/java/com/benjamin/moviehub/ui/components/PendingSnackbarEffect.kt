package com.benjamin.moviehub.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Shows [message] while [pending] is true and acknowledges it only once the snackbar has finished
 * displaying. When the caller leaves composition while the message is visible, the pending state
 * survives and the message is shown again on return; repeated results share one message.
 */
@Composable
internal fun PendingSnackbarEffect(
    pending: Boolean,
    snackbarHostState: SnackbarHostState,
    message: String,
    onAcknowledged: () -> Unit,
) {
    LaunchedEffect(pending, message) {
        if (pending) {
            snackbarHostState.showSnackbar(message)
            onAcknowledged()
        }
    }
}
