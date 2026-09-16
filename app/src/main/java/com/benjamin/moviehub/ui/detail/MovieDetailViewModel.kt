package com.benjamin.moviehub.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()
        private val _libraryActionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val libraryActionErrors = _libraryActionErrors.asSharedFlow()
        private var loadJob: Job? = null
        private var libraryJob: Job? = null
        private val libraryMutex = Mutex()
        private var latestLibraryById: Map<Int, Movie> = emptyMap()

        fun loadMovieDetails(movieId: Int) {
            val current = _uiState.value
            if (current is MovieDetailUiState.Success && current.movie.id == movieId) return

            loadJob?.cancel()
            libraryJob?.cancel()
            loadJob =
                viewModelScope.launch {
                    _uiState.value = MovieDetailUiState.Loading
                    try {
                        coroutineScope {
                            val credits = async { loadCredits(movieId) }
                            val recommendations = async { loadRecommendations(movieId) }
                            // A main failure throws out of the scope and cancels both async children.
                            val movie = repository.getMovieDetails(movieId)

                            _uiState.value = MovieDetailUiState.Success(movie, MovieCredits())
                            // Runs in viewModelScope on purpose: the observer must outlive the load
                            // job so library changes made elsewhere keep reconciling this screen.
                            libraryJob = viewModelScope.launch { observeLibrary(movieId) }

                            val loadedCredits = credits.await()
                            updateSuccess(movieId) { it.copy(credits = loadedCredits) }
                            val loadedRecommendations = recommendations.await()
                            updateSuccess(movieId) {
                                it.copy(recommendations = loadedRecommendations.withLocalFlags(latestLibraryById))
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _uiState.value = MovieDetailUiState.Error(R.string.error_loading_movie_detail)
                    }
                }
        }

        fun toggleFavorite() = toggleLibraryFlag(LibraryFlag.FAVORITE)

        fun toggleWatchlist() = toggleLibraryFlag(LibraryFlag.WATCHLIST)

        fun toggleWatched() = toggleLibraryFlag(LibraryFlag.WATCHED)

        private fun toggleLibraryFlag(flag: LibraryFlag) {
            viewModelScope.launch {
                libraryMutex.withLock {
                    val state = _uiState.value as? MovieDetailUiState.Success ?: return@withLock
                    val previous = state.movie
                    val value = !flag.isSet(previous)
                    _uiState.value = state.copy(movie = flag.apply(previous, value))

                    try {
                        flag.persist(repository, previous, value)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _libraryActionErrors.tryEmit(Unit)
                        rollbackLibrary(previous.id, previous, flag, value)
                    }
                }
            }
        }

        private suspend fun loadCredits(movieId: Int): MovieCredits =
            try {
                repository.getMovieCredits(movieId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                MovieCredits()
            }

        private suspend fun loadRecommendations(movieId: Int): MovieRecommendationsUiState =
            try {
                repository
                    .getMovieRecommendations(movieId)
                    .takeIf(List<Movie>::isNotEmpty)
                    ?.let(MovieRecommendationsUiState::Success)
                    ?: MovieRecommendationsUiState.Empty
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                MovieRecommendationsUiState.Error
            }

        private suspend fun observeLibrary(movieId: Int) {
            repository.getLibraryMovies().collectLatest { localMovies ->
                val localById = localMovies.associateBy { it.id }
                latestLibraryById = localById
                updateSuccess(movieId) { it.withLocalFlags(localById) }
            }
        }

        private fun updateSuccess(
            movieId: Int,
            transform: (MovieDetailUiState.Success) -> MovieDetailUiState.Success,
        ) {
            val latest = _uiState.value as? MovieDetailUiState.Success ?: return
            if (latest.movie.id == movieId) _uiState.value = transform(latest)
        }

        private fun rollbackLibrary(
            movieId: Int,
            previous: Movie,
            flag: LibraryFlag,
            value: Boolean,
        ) {
            updateSuccess(movieId) { latest ->
                if (flag.isSet(latest.movie) != value) {
                    latest
                } else {
                    latest.copy(movie = flag.restore(latest.movie, previous, value))
                }
            }
        }
    }

/**
 * One library flag and its responsibilities: read it, apply it optimistically
 * (watchlist and watched stay mutually exclusive) and restore the previous flags when
 * persistence fails.
 */
private enum class LibraryFlag(
    val isSet: (Movie) -> Boolean,
    val apply: (Movie, Boolean) -> Movie,
    val restore: (Movie, Movie, Boolean) -> Movie,
    val persist: suspend (MovieRepository, Movie, Boolean) -> Unit,
) {
    FAVORITE(
        isSet = Movie::isFavorite,
        apply = { movie, value -> movie.copy(isFavorite = value) },
        restore = { movie, previous, _ -> movie.copy(isFavorite = previous.isFavorite) },
        persist = { repository, movie, value -> repository.setFavorite(movie, value) },
    ),
    WATCHLIST(
        isSet = Movie::isWatchlist,
        apply = { movie, value -> movie.copy(isWatchlist = value, isWatched = if (value) false else movie.isWatched) },
        restore = { movie, previous, value ->
            movie.copy(isWatchlist = previous.isWatchlist, isWatched = if (value) previous.isWatched else movie.isWatched)
        },
        persist = { repository, movie, value -> repository.setWatchlist(movie, value) },
    ),
    WATCHED(
        isSet = Movie::isWatched,
        apply = { movie, value -> movie.copy(isWatched = value, isWatchlist = if (value) false else movie.isWatchlist) },
        restore = { movie, previous, value ->
            movie.copy(isWatched = previous.isWatched, isWatchlist = if (value) previous.isWatchlist else movie.isWatchlist)
        },
        persist = { repository, movie, value -> repository.setWatched(movie, value) },
    ),
}

private fun MovieRecommendationsUiState.withLocalFlags(localById: Map<Int, Movie>): MovieRecommendationsUiState =
    (this as? MovieRecommendationsUiState.Success)
        ?.let { success ->
            MovieRecommendationsUiState.Success(success.movies.map { it.withFlags(localById[it.id]) })
        } ?: this

private fun MovieDetailUiState.Success.withLocalFlags(localById: Map<Int, Movie>): MovieDetailUiState.Success =
    copy(
        movie = movie.withFlags(localById[movie.id]),
        recommendations = recommendations.withLocalFlags(localById),
    )

private fun Movie.withFlags(local: Movie?): Movie =
    copy(
        isFavorite = local?.isFavorite ?: false,
        isWatchlist = local?.isWatchlist ?: false,
        isWatched = local?.isWatched ?: false,
    )

sealed class MovieDetailUiState {
    data object Loading : MovieDetailUiState()

    data class Success(
        val movie: Movie,
        val credits: MovieCredits,
        val recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Loading,
    ) : MovieDetailUiState()

    data class Error(
        @param:StringRes val errorMessage: Int,
    ) : MovieDetailUiState()
}

sealed interface MovieRecommendationsUiState {
    data object Loading : MovieRecommendationsUiState

    data class Success(
        val movies: List<Movie>,
    ) : MovieRecommendationsUiState

    data object Empty : MovieRecommendationsUiState

    data object Error : MovieRecommendationsUiState
}
