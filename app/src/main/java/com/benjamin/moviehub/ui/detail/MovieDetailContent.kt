package com.benjamin.moviehub.ui.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.theme.MovieHubTheme
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.ui.components.EmptyStateView
import com.benjamin.moviehub.ui.components.previewMovie

@Composable
fun MovieDetailContent(
    movie: Movie,
    credits: MovieCreditsUiState,
    onToggleFavorite: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onToggleWatched: () -> Unit,
    onRecommendationClick: (Int) -> Unit,
    onRetryCredits: () -> Unit,
    onRetryRecommendations: () -> Unit,
    onRetryLibraryObservation: () -> Unit,
    modifier: Modifier = Modifier,
    recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Empty,
    listState: LazyListState = rememberLazyListState(),
    isLibraryActionPending: Boolean = false,
    libraryObservation: LibraryObservationUiState = LibraryObservationUiState.Ready,
    onOpenTmdb: (() -> Unit)? = null,
) {
    // Edge-to-edge insets: the system bottom inset goes into contentPadding, not the
    // Modifier, so content scrolls behind the bars instead of being clipped.
    val navigationInsets = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    val libraryActionsEnabled = !isLibraryActionPending && libraryObservation is LibraryObservationUiState.Ready
    val creditsSuccess = credits as? MovieCreditsUiState.Success
    LazyColumn(
        modifier = modifier.testTag("detail_content_list"),
        state = listState,
        contentPadding =
            PaddingValues(
                start = navigationInsets.calculateLeftPadding(layoutDirection),
                end = navigationInsets.calculateRightPadding(layoutDirection),
                bottom = 24.dp + navigationInsets.calculateBottomPadding(),
            ),
    ) {
        item(key = "header") {
            MovieDetailHeader(
                movie = movie,
                director = creditsSuccess?.credits?.director?.takeIf(String::isNotBlank),
                libraryActionsEnabled = libraryActionsEnabled,
                onToggleFavorite = onToggleFavorite,
                onToggleWatchlist = onToggleWatchlist,
                onToggleWatched = onToggleWatched,
                onOpenTmdb = onOpenTmdb,
            )
        }

        if (libraryObservation is LibraryObservationUiState.Error) {
            item(key = "library_sync_error") {
                Box(
                    modifier = Modifier.fillMaxWidth().testTag("library_sync_error"),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(modifier = Modifier.widthIn(max = DetailContentMaxWidth).fillMaxWidth()) {
                        EmptyStateView(
                            message = stringResource(R.string.error_syncing_library),
                            onRetry = onRetryLibraryObservation,
                            compact = true,
                        )
                    }
                }
            }
        }

        if (movie.overview.isNotBlank()) {
            item(key = "synopsis") {
                DetailSection(
                    title = stringResource(R.string.synopsis),
                ) {
                    SynopsisText(movieId = movie.id, overview = movie.overview)
                }
            }
        }

        if (credits is MovieCreditsUiState.Success && credits.credits.actors.isNotEmpty()) {
            item(key = "cast") {
                MovieCreditsSection(credits = credits.credits)
            }
        } else if (credits is MovieCreditsUiState.Error) {
            item(key = "cast_error") {
                MovieCreditsErrorSection(onRetry = onRetryCredits)
            }
        }

        when (recommendations) {
            MovieRecommendationsUiState.Loading -> {
                item(key = "recommendations") {
                    MovieRecommendationsLoadingSection()
                }
            }

            is MovieRecommendationsUiState.Success -> {
                item(key = "recommendations") {
                    MovieRecommendationsSection(
                        movies = recommendations.movies,
                        onMovieClick = onRecommendationClick,
                    )
                }
            }

            is MovieRecommendationsUiState.Error -> {
                item(key = "recommendations_error") {
                    MovieRecommendationsErrorSection(onRetry = onRetryRecommendations)
                }
            }

            MovieRecommendationsUiState.Empty -> Unit
        }
    }
}

@PreviewLightDark
@Composable
private fun MovieDetailContentPreview() {
    MovieHubTheme {
        MovieDetailContent(
            movie = previewMovie().copy(runtimeMinutes = 124, voteCount = 1200),
            credits = MovieCreditsUiState.Success(MovieCredits(director = "James Cameron")),
            onToggleFavorite = {},
            onToggleWatchlist = {},
            onToggleWatched = {},
            onRecommendationClick = {},
            onRetryCredits = {},
            onRetryRecommendations = {},
            onRetryLibraryObservation = {},
        )
    }
}
