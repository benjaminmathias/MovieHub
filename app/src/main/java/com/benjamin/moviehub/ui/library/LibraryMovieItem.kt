package com.benjamin.moviehub.ui.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.ui.components.CompactMovieItem
import com.benjamin.moviehub.ui.components.DeleteBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LibraryMovieItem(
    movie: Movie,
    onMovieClick: (Int) -> Unit,
    onRemove: (Movie) -> Unit,
    removeLabel: String,
    removeIcon: ImageVector,
    modifier: Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onRemove(movie)
            dismissState.reset()
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier =
            modifier.fillMaxWidth().semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction(removeLabel) {
                            onRemove(movie)
                            true
                        },
                    )
            },
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled || dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                DeleteBackground(icon = removeIcon)
            }
        },
        content = { CompactMovieItem(movie = movie, onMovieClick = onMovieClick) },
    )
}
