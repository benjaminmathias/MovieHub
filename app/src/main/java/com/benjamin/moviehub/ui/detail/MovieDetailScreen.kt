package com.benjamin.moviehub.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.components.EmptyStateView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetry: () -> Unit,
    favoriteActionErrors: Flow<Unit> = emptyFlow(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val favoriteErrorMessage = stringResource(R.string.error_updating_favorite)
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // 0f (sur le hero) -> 1f (contenu scrollé) : évite le flash opaque dès 1px scrollé.
    val toolbarProgress by remember(density) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                val maxOffset = with(density) { 180.dp.toPx() }.coerceAtLeast(1f)
                (listState.firstVisibleItemScrollOffset / maxOffset).coerceIn(0f, 1f)
            }
        }
    }
    val toolbarVisible = toolbarProgress > 0.7f
    val detailTitle = (uiState as? MovieDetailUiState.Success)?.movie?.title.orEmpty()

    LaunchedEffect(favoriteActionErrors, favoriteErrorMessage) {
        favoriteActionErrors.collect {
            snackbarHostState.showSnackbar(favoriteErrorMessage)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("detail_screen"),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = toolbarProgress))
                    .statusBarsPadding()
                    .zIndex(1f)
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = -1f
                    },
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailControlButton(
                    contentDescription = stringResource(R.string.back),
                    onClick = onBackClick,
                    elevated = !toolbarVisible,
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                if (detailTitle.isNotBlank()) {
                    Text(
                        text = detailTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .graphicsLayer { alpha = toolbarProgress }
                                .then(
                                    if (toolbarVisible) Modifier else Modifier.clearAndSetSemantics {},
                                ),
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                if (uiState is MovieDetailUiState.Success) {
                    DetailControlButton(
                        contentDescription = stringResource(R.string.share),
                        onClick = { shareMovie(context, uiState.movie) },
                        elevated = !toolbarVisible,
                    ) {
                        Icon(imageVector = Icons.Outlined.Share, contentDescription = null)
                    }
                }
            }
            if (toolbarVisible) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            } else {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }

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
                    listState = listState,
                    onToggleFavorite = onToggleFavorite,
                    onOpenTmdb =
                        uiState.movie.webUrl
                            ?.takeIf(::isValidHttpUrl)
                            ?.let { url -> { openMovieInBrowser(context, url) } },
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

@Composable
private fun DetailControlButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .size(48.dp)
                .semantics {
                    this.contentDescription = contentDescription
                },
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (elevated) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                        } else {
                            Color.Transparent
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}
