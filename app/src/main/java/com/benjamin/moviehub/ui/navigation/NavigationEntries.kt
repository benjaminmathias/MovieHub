package com.benjamin.moviehub.ui.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.benjamin.moviehub.R
import com.benjamin.moviehub.ui.detail.MovieDetailScreen
import com.benjamin.moviehub.ui.detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.discover.DiscoverScreen
import com.benjamin.moviehub.ui.discover.DiscoverViewModel
import com.benjamin.moviehub.ui.favorites.FavoriteScreen
import com.benjamin.moviehub.ui.favorites.FavoriteViewModel
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
    val heroMovie by viewModel.heroMovie.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.favoriteActionErrors.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.error_updating_favorite))
        }
    }

    MovieListScreen(
        categoryMovies = viewModel.categoryMovies,
        heroMovie = heroMovie,
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

    SearchScreen(
        searchResults = viewModel.searchResults,
        searchQuery = searchQuery,
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
        onReleaseYearSelected = viewModel::onReleaseYearSelected,
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

    LaunchedEffect(movieId) {
        viewModel.loadMovieDetails(movieId)
    }

    MovieDetailScreen(
        uiState = detailsUiState,
        onBackClick = onBack,
        onToggleFavorite = viewModel::toggleFavorite,
        onRetry = { viewModel.loadMovieDetails(movieId) },
        favoriteActionErrors = viewModel.favoriteActionErrors,
        onRecommendationClick = onOpenRecommendation,
    )
}

@Composable
internal fun FavoriteListEntry(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetails: (Int) -> Unit,
) {
    val viewModel: FavoriteViewModel = hiltViewModel()
    val favoriteUiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoriteScreen(
        state = favoriteUiState,
        onBackClick = onBack,
        onSettingsClick = onOpenSettings,
        onMovieClick = onOpenDetails,
        onRemoveFavorite = viewModel::onToggleFavorite,
        onRetry = viewModel::onRetry,
        favoriteActionErrors = viewModel.favoriteActionErrors,
    )
}

@Composable
internal fun SettingsEntry(onBack: () -> Unit) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isClearing by viewModel.isClearing.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel.imageCacheMessages) {
        viewModel.imageCacheMessages.collect { cleared ->
            snackbarHostState.showSnackbar(
                context.getString(
                    if (cleared) R.string.image_cache_cleared else R.string.image_cache_clear_failed,
                ),
            )
        }
    }
    LaunchedEffect(viewModel.themeUpdateErrors) {
        viewModel.themeUpdateErrors.collect {
            snackbarHostState.showSnackbar(context.getString(R.string.theme_update_failed))
        }
    }

    SettingsScreen(
        currentTheme = currentTheme,
        isClearing = isClearing,
        snackbarHostState = snackbarHostState,
        onThemeSelected = viewModel::updateTheme,
        onClearImageCache = viewModel::clearImageCache,
        onBackClick = onBack,
    )
}
