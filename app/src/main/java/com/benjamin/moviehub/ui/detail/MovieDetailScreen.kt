package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.components.EmptyStateView

@Composable
fun MovieDetailScreen(
    uiState: MovieDetailUiState,
    onBackClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRetry: () -> Unit,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).testTag("detail_screen"),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (isScrolled) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            Color.Transparent
                        },
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp)
                    .zIndex(1f)
                    .semantics {
                        isTraversalGroup = true
                        traversalIndex = -1f
                    },
            verticalAlignment = Alignment.Top,
        ) {
            DetailControlButton(
                contentDescription = stringResource(R.string.back),
                onClick = onBackClick,
                isScrolled = isScrolled,
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            Spacer(modifier = Modifier.weight(1f))
            if (uiState is MovieDetailUiState.Success) {
                DetailControlButton(
                    contentDescription = stringResource(R.string.share),
                    onClick = { shareMovie(context, uiState.movie) },
                    isScrolled = isScrolled,
                ) {
                    Icon(imageVector = Icons.Outlined.Share, contentDescription = null)
                }
                DetailControlButton(
                    contentDescription =
                        stringResource(
                            if (uiState.movie.isFavorite) R.string.remove_favorite else R.string.favorite,
                        ),
                    modifier = Modifier.testTag("detail_favorite"),
                    onClick = onToggleFavorite,
                    isScrolled = isScrolled,
                ) {
                    Icon(
                        imageVector =
                            if (uiState.movie.isFavorite) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                        contentDescription = null,
                        tint =
                            if (uiState.movie.isFavorite) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    )
                }
            }
        }

        when (uiState) {
            MovieDetailUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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
                    onToggleFavorite = onToggleFavorite,
                    listState = listState,
                )
            }

            is MovieDetailUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    EmptyStateView(
                        message = uiState.errorMessage.asString(),
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailControlButton(
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isScrolled: Boolean,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier =
            Modifier
                .then(modifier)
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
                        if (isScrolled) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
                        },
                    ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
    }
}
