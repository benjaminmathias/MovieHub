package com.benjamin.moviehub.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.benjamin.moviehub.R

/**
 * Renders the coarse [PagingPhase] of a feed with shared error and empty states,
 * delegating the loading placeholders and the populated content to the caller.
 */
@Composable
internal fun PagingStatus(
    phase: PagingPhase,
    onRetry: () -> Unit,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector = Icons.Default.SearchOff,
    loading: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    when (phase) {
        PagingPhase.LOADING -> loading()
        PagingPhase.ERROR ->
            EmptyStateView(
                message = stringResource(R.string.error_loading_movies),
                icon = Icons.Default.CloudOff,
                onRetry = onRetry,
                modifier = modifier,
            )

        PagingPhase.EMPTY -> EmptyStateView(message = emptyMessage, icon = emptyIcon, modifier = modifier)
        PagingPhase.CONTENT -> content()
    }
}
