package com.benjamin.moviehub.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R

/**
 * Shared retry action so every "Réessayer" entry point in the app stays visually identical.
 * Colors are pinned explicitly so the neutral tonal look never depends on a themed
 * `secondaryContainer` that could blend into elevated surfaces.
 */
@Composable
internal fun RetryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors =
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
    ) {
        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(text = stringResource(R.string.retry))
    }
}
