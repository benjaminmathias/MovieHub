package com.benjamin.moviehub.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.domain.repository.setLibraryFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
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
        private val libraryById = MutableStateFlow<Map<Int, Movie>>(emptyMap())

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
                            startObservingLibrary(movieId)

                            launch {
                                val loadedCredits = credits.await()
                                updateSuccess(movieId) { it.copy(credits = loadedCredits) }
                            }
                            launch {
                                val loadedRecommendations = recommendations.await()
                                updateSuccess(movieId) {
                                    it.copy(recommendations = loadedRecommendations.withLibraryState(libraryById.value))
                                }
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
                    _uiState.value = state.copy(movie = flag.apply(previous, value), isLibraryActionPending = true)

                    try {
                        repository.setLibraryFlag(previous, flag, value)
                        setLibraryActionPending(previous.id, false)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _libraryActionErrors.tryEmit(Unit)
                        rollbackLibrary(previous.id, previous, flag, value)
                        setLibraryActionPending(previous.id, false)
                    }
                }
            }
        }

        /** Keeps the library actions locked while a local write is in flight. */
        private fun setLibraryActionPending(
            movieId: Int,
            pending: Boolean,
        ) {
            updateSuccess(movieId) { it.copy(isLibraryActionPending = pending) }
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
                    ?.let { MovieRecommendationsUiState.Success(it.toImmutableList()) }
                    ?: MovieRecommendationsUiState.Empty
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                MovieRecommendationsUiState.Error
            }

        /**
         * Observes the library in [viewModelScope] so the observer outlives the load job:
         * library changes made on other screens keep reconciling this one.
         */
        private fun startObservingLibrary(movieId: Int) {
            libraryJob =
                viewModelScope.launch {
                    repository.getLibraryMovies().collectLatest { localMovies ->
                        val localById = localMovies.associateBy(Movie::id)
                        libraryById.value = localById
                        updateSuccess(movieId) { it.withLibraryState(localById) }
                    }
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
 * Reconciles the detail screen with the library, which is the source of truth here:
 * a movie absent from [localById] loses its flags.
 *
 * This deliberately differs from [com.benjamin.moviehub.data.mapper.withLocalFlags],
 * which keeps the remote flags for a not-yet-cached movie in network feeds.
 */
private fun MovieRecommendationsUiState.withLibraryState(localById: Map<Int, Movie>): MovieRecommendationsUiState =
    (this as? MovieRecommendationsUiState.Success)
        ?.let { success ->
            MovieRecommendationsUiState.Success(success.movies.map { it.withLibraryState(localById[it.id]) }.toImmutableList())
        } ?: this

private fun MovieDetailUiState.Success.withLibraryState(localById: Map<Int, Movie>): MovieDetailUiState.Success =
    copy(
        movie = movie.withLibraryState(localById[movie.id]),
        recommendations = recommendations.withLibraryState(localById),
    )

private fun Movie.withLibraryState(local: Movie?): Movie =
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
        val isLibraryActionPending: Boolean = false,
    ) : MovieDetailUiState()

    data class Error(
        @param:StringRes val errorMessage: Int,
    ) : MovieDetailUiState()
}

sealed interface MovieRecommendationsUiState {
    data object Loading : MovieRecommendationsUiState

    data class Success(
        val movies: ImmutableList<Movie>,
    ) : MovieRecommendationsUiState

    data object Empty : MovieRecommendationsUiState

    data object Error : MovieRecommendationsUiState
}
