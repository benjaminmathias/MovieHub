package com.benjamin.moviehub.ui.detail

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.ContentHorizontalPadding
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.core.theme.POSTER_ASPECT_RATIO
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.previewMovie
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

private val SkeletonPosterWidth = 112.dp
private val SkeletonChipHeight = 48.dp
private val SkeletonSectionSpacing = 20.dp

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
    // 0f (on the hero) -> 1f (content scrolled): avoids an opaque flash after a 1px scroll.
    val toolbarProgress =
        remember(listState, density) {
            derivedStateOf {
                if (listState.firstVisibleItemIndex > 0) {
                    1f
                } else {
                    val maxOffset = with(density) { 180.dp.toPx() }.coerceAtLeast(1f)
                    (listState.firstVisibleItemScrollOffset / maxOffset).coerceIn(0f, 1f)
                }
            }
        }
    val toolbarVisible by remember(toolbarProgress) { derivedStateOf { toolbarProgress.value > 0.7f } }
    val detailTitle = (uiState as? MovieDetailUiState.Success)?.movie?.title.orEmpty()

    LaunchedEffect(libraryActionErrors, libraryErrorMessage) {
        libraryActionErrors.collect { snackbarHostState.showSnackbar(libraryErrorMessage) }
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
            MovieDetailUiState.Loading -> MovieDetailSkeleton(modifier = Modifier.fillMaxSize())

            is MovieDetailUiState.Success -> {
                MovieDetailContent(
                    movie = uiState.movie,
                    credits = uiState.credits,
                    recommendations = uiState.recommendations,
                    listState = listState,
                    isLibraryActionPending = uiState.isLibraryActionPending,
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

@Composable
private fun DetailToolbar(
    title: String,
    toolbarVisible: Boolean,
    progressProvider: () -> Float,
    onBackClick: () -> Unit,
    onShareClick: (() -> Unit)?,
) {
    val toolbarColor = MaterialTheme.colorScheme.surface
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind { drawRect(toolbarColor.copy(alpha = progressProvider())) }
                .statusBarsPadding()
                .zIndex(1f)
                .semantics {
                    isTraversalGroup = true
                    traversalIndex = -1f
                },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DetailControlButton(
                contentDescription = stringResource(R.string.back),
                onClick = onBackClick,
                elevated = !toolbarVisible,
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .graphicsLayer { alpha = progressProvider() }
                            .then(if (toolbarVisible) Modifier else Modifier.clearAndSetSemantics {}),
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            onShareClick?.let { share ->
                DetailControlButton(
                    contentDescription = stringResource(R.string.share),
                    onClick = share,
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
        modifier = modifier.size(48.dp).semantics { this.contentDescription = contentDescription },
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

/** Structured placeholder mirroring the loaded layout while the first payload arrives. */
@Composable
private fun MovieDetailSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.loading_movie_details)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .testTag("detail_skeleton")
                .semantics { contentDescription = loadingDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SkeletonBlock(modifier = Modifier.fillMaxWidth().height(DetailHeroHeight))
        Column(
            modifier =
                Modifier
                    .widthIn(max = DetailContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = ContentHorizontalPadding, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(SkeletonSectionSpacing),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SkeletonBlock(
                    modifier = Modifier.width(SkeletonPosterWidth).aspectRatio(POSTER_ASPECT_RATIO),
                    shape = MaterialTheme.shapes.medium,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.9f).height(22.dp))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f))
                    SkeletonBlock(modifier = Modifier.fillMaxWidth(0.65f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) {
                    SkeletonBlock(
                        modifier = Modifier.weight(1f).height(SkeletonChipHeight),
                        shape = MaterialTheme.shapes.small,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(modifier = Modifier.width(120.dp).height(20.dp))
                SkeletonBlock(modifier = Modifier.fillMaxWidth())
                SkeletonBlock(modifier = Modifier.fillMaxWidth())
                SkeletonBlock(modifier = Modifier.fillMaxWidth(0.6f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SkeletonBlock(modifier = Modifier.width(140.dp).height(20.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(count = 4) {
                        SkeletonBlock(modifier = Modifier.size(80.dp), shape = CircleShape)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(modifier = modifier.heightIn(min = 14.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape).shimmer())
}

private fun isValidHttpUrl(url: String): Boolean {
    val uri = url.trim().toUri()
    return uri.host?.isNotBlank() == true && uri.scheme?.lowercase() in setOf("http", "https")
}

private fun openMovieInBrowser(
    context: Context,
    url: String,
) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}

/** Shares a movie through an intent, with the title and matching TMDB url. */
internal fun shareMovie(
    context: Context,
    movie: Movie,
) {
    val sendIntent =
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(
                Intent.EXTRA_TEXT,
                context.getString(R.string.share_movie_text, movie.title, movie.webUrl ?: ""),
            )
            type = "text/plain"
        }
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.share_chooser)))
}

@PreviewLightDark
@PreviewFontScale
@Composable
private fun MovieDetailScreenPreview() {
    MovieHubTheme {
        MovieDetailScreen(
            uiState =
                MovieDetailUiState.Success(
                    movie = previewMovie().copy(runtimeMinutes = 124, voteCount = 1200),
                    credits = MovieCredits(director = "James Cameron"),
                ),
            onBackClick = {},
            onToggleFavorite = {},
            onToggleWatchlist = {},
            onToggleWatched = {},
            onRetry = {},
        )
    }
}
