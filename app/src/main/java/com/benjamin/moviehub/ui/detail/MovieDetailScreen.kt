package com.benjamin.moviehub.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.components.EmptyStateView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleWatchlist: () -> Unit = {},
    onToggleWatched: () -> Unit = {},
    onRetry: () -> Unit,
    libraryActionErrors: Flow<Unit> = emptyFlow(),
    onRecommendationClick: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val libraryErrorMessage = stringResource(R.string.error_updating_library)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // 0f (sur le hero) -> 1f (contenu scrollé) : évite le flash opaque dès 1px scrollé.
    val toolbarProgress = remember(density) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val maxOffset = with(density) { 180.dp.toPx() }.coerceAtLeast(1f)
                (listState.firstVisibleItemScrollOffset / maxOffset).coerceIn(0f, 1f)
            }
        }
    }
    val toolbarVisible by remember {
        derivedStateOf { toolbarProgress.value > 0.7f }
    }
    val detailTitle = (uiState as? MovieDetailUiState.Success)?.movie?.title.orEmpty()

    LaunchedEffect(libraryActionErrors, libraryErrorMessage) {
        libraryActionErrors.collect {
            snackbarHostState.showSnackbar(libraryErrorMessage)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("detail_screen"),
    ) {
        DetailToolbar(
            title = detailTitle,
            toolbarVisible = toolbarVisible,
            progressProvider = { toolbarProgress.value },
            onBackClick = onBackClick,
            onShareClick =
                (uiState as? MovieDetailUiState.Success)?.let { success ->
                    { shareMovie(context, success.movie) }
                },
        )

        when (uiState) {
            MovieDetailUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = stringResource(R.string.loading_movie_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is MovieDetailUiState.Success -> {
                MovieDetailContent(
                    movie = uiState.movie,
                    credits = uiState.credits,
                    recommendations = uiState.recommendations,
                    listState = listState,
                    onToggleFavorite = onToggleFavorite,
                    onToggleWatchlist = onToggleWatchlist,
                    onToggleWatched = onToggleWatched,
                    onOpenTmdb =
                        uiState.movie.webUrl
                            ?.takeIf(::isValidHttpUrl)
                            ?.let { url -> { openMovieInBrowser(context, url) } },
                    onRecommendationClick = onRecommendationClick,
                )
            }

            is MovieDetailUiState.Error -> {
                EmptyStateView(
                    message = stringResource(uiState.errorMessage),
                    onRetry = onRetry,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

private fun isValidHttpUrl(url: String): Boolean {
    val uri = Uri.parse(url.trim())
    return uri.host?.isNotBlank() == true && uri.scheme?.lowercase() in setOf("http", "https")
}

private fun openMovieInBrowser(
    context: Context,
    url: String,
) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
