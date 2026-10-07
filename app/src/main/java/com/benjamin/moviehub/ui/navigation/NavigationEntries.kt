package com.benjamin.moviehub.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.components.PendingSnackbarEffect
import com.benjamin.moviehub.ui.detail.MovieDetailScreen
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.discover.DiscoverScreen
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import com.benjamin.moviehub.ui.library.LibraryScreen
import com.benjamin.moviehub.ui.library.LibraryViewModel
import com.benjamin.moviehub.ui.list.MovieListScreen
import com.benjamin.moviehub.ui.list.MovieListViewModel
import com.benjamin.moviehub.ui.search.SearchScreen
import com.benjamin.moviehub.ui.search.SearchViewModel
import com.benjamin.moviehub.ui.settings.SettingsScreen
import com.benjamin.moviehub.ui.settings.SettingsViewModel

@Composable
internal fun MovieListEntry(
    onOpenDetails: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel: MovieListViewModel = hiltViewModel()
    val heroState by viewModel.heroMovieState.collectAsStateWithLifecycle()
    val favoriteErrorPending by viewModel.favoriteErrorPending.collectAsStateWithLifecycle()
    val favoriteErrorMessage = stringResource(R.string.error_updating_favorite)

    PendingSnackbarEffect(
        pending = favoriteErrorPending,
        snackbarHostState = snackbarHostState,
        message = favoriteErrorMessage,
        onAcknowledged = viewModel::acknowledgeFavoriteError,
    )

    MovieListScreen(
        categoryMovies = viewModel.categoryMovies,
        heroState = heroState,
        onRetryHero = viewModel::retryHero,
        onMovieClick = onOpenDetails,
        onToggleFavorite = viewModel::onToggleFavorite,
        onSearchClick = onOpenSearch,
        onSettingsClick = onOpenSettings,
    )
}

@Composable
internal fun SearchEntry(
    onBack: () -> Unit,
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeSearchQuery by viewModel.activeSearchQuery.collectAsStateWithLifecycle()

    SearchScreen(
        searchResults = viewModel.searchResults,
        searchQuery = searchQuery,
        activeSearchQuery = activeSearchQuery,
        onSearchChanged = viewModel::onSearchQueryChanged,
        onMovieClick = onOpenDetails,
        onBack = onBack,
    )
}

@Composable
internal fun DiscoverEntry(onOpenDetails: (Int) -> Unit) {
    val viewModel: DiscoverViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DiscoverScreen(
        state = state,
        discoverResults = viewModel.discoverResults,
        onGenreSelected = viewModel::onGenreSelected,
        onReleaseDecadeSelected = viewModel::onReleaseDecadeSelected,
        onMinimumRatingSelected = viewModel::onMinimumRatingSelected,
        onSortSelected = viewModel::onSortSelected,
        onBeginFilterEditing = viewModel::beginFilterEditing,
        onApplyFilters = viewModel::applyFilters,
        onResetFilters = viewModel::resetFilters,
        onDiscardFilterEdits = viewModel::discardFilterEdits,
        onRetryGenres = viewModel::retryGenres,
        onMovieClick = onOpenDetails,
    )
}

@Composable
internal fun MovieDetailEntry(
    movieId: Int,
    onBack: () -> Unit,
    onOpenRecommendation: (Int) -> Unit,
) {
    val viewModel: MovieDetailViewModel = hiltViewModel()
    val detailsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val libraryActionErrorPending by viewModel.libraryActionErrorPending.collectAsStateWithLifecycle()

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }

    MovieDetailScreen(
        uiState = detailsUiState,
        onBackClick = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onToggleWatchlist = viewModel::toggleWatchlist,
        onToggleWatched = viewModel::toggleWatched,
        onRetry = { viewModel.loadMovieDetails(movieId) },
        onRetryCredits = viewModel::retryCredits,
        onRetryRecommendations = viewModel::retryRecommendations,
        onRetryLibraryObservation = viewModel::retryLibraryObservation,
        libraryActionErrorPending = libraryActionErrorPending,
        onLibraryActionErrorAcknowledged = viewModel::acknowledgeLibraryActionError,
        onRecommendationClick = onOpenRecommendation,
    )
}

@Composable
internal fun LibraryEntry(
    onOpenSettings: () -> Unit,
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: LibraryViewModel = hiltViewModel()
    val libraryUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actionErrorPending by viewModel.actionErrorPending.collectAsStateWithLifecycle()

    LibraryScreen(
        state = libraryUiState,
        onSettingsClick = onOpenSettings,
        onMovieClick = onOpenDetails,
        onRemove = viewModel::onRemove,
        onRetry = viewModel::onRetry,
        actionErrorPending = actionErrorPending,
        onActionErrorAcknowledged = viewModel::acknowledgeActionError,
    )
}

@Composable
internal fun SettingsEntry(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val imageCacheResult by viewModel.imageCacheResult.collectAsStateWithLifecycle()
    val themeUpdateErrorPending by viewModel.themeUpdateErrorPending.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val imageCacheClearedMessage = stringResource(R.string.image_cache_cleared)
    val imageCacheClearFailedMessage = stringResource(R.string.image_cache_clear_failed)
    val themeUpdateFailedMessage = stringResource(R.string.theme_update_failed)

    imageCacheResult?.let { result ->
        PendingSnackbarEffect(
            pending = true,
            snackbarHostState = snackbarHostState,
            message = if (result) imageCacheClearedMessage else imageCacheClearFailedMessage,
            onAcknowledged = { viewModel.acknowledgeImageCacheResult(result) },
        )
    }
    PendingSnackbarEffect(
        pending = themeUpdateErrorPending,
        snackbarHostState = snackbarHostState,
        message = themeUpdateFailedMessage,
        onAcknowledged = viewModel::acknowledgeThemeUpdateError,
    )

    SettingsScreen(
        currentTheme = currentTheme,
        isClearing = isClearing,
        snackbarHostState = snackbarHostState,
        onThemeSelected = viewModel::updateTheme,
        onClearImageCache = viewModel::clearImageCache,
        onBackClick = onBack,
    )
}
