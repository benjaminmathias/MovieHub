package com.benjamin.moviehub.ui.detail

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.LibraryFlag
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.LibraryRepository
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.domain.repository.setLibraryFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class MovieDetailViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
        private val libraryRepository: LibraryRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()

        /** A library write failed and its message has not been shown yet. */
        private val _libraryActionErrorPending = MutableStateFlow(false)
        val libraryActionErrorPending: StateFlow<Boolean> = _libraryActionErrorPending.asStateFlow()
        private var loadJob: Job? = null
        private var loadingMovieId: Int? = null
        private var libraryJob: Job? = null
        private var creditsRetryJob: Job? = null
        private var recommendationsRetryJob: Job? = null
        private val libraryMutex = Mutex()

        // Null until a library snapshot has been observed: an unknown library must not be
        // treated as an empty (authoritative) one, or a failed read would clear live flags.
        private var libraryById: Map<Int, Movie>? = null

        fun loadMovieDetails(movieId: Int) {
            val current = _uiState.value
            if (current is MovieDetailUiState.Success && current.movie.id == movieId) return
            // A load for the same movie is already scheduled or running: keep it instead of
            // restarting the work, including before the launched coroutine body has started.
            if (loadingMovieId == movieId && loadJob?.isActive == true) return

            cancelSectionWork()
            libraryById = null
            loadingMovieId = movieId
            loadJob =
                viewModelScope.launch {
                    _uiState.value = MovieDetailUiState.Loading
                    try {
                        coroutineScope {
                            val credits = async { loadCredits(movieId) }
                            val recommendations = async { loadRecommendations(movieId) }
                            // A main failure throws out of the scope and cancels both async children.
                            val movie = repository.getMovieDetails(movieId)

                            _uiState.value =
                                MovieDetailUiState.Success(
                                    movie = movie,
                                    credits = MovieCreditsUiState.Loading,
                                    recommendations = MovieRecommendationsUiState.Loading,
                                    libraryObservation = LibraryObservationUiState.Loading,
                                )
                            startObservingLibrary(movieId)

                            launch {
                                val loadedCredits = credits.await()
                                updateSuccess(movieId) { it.copy(credits = loadedCredits) }
                            }
                            launch {
                                val loadedRecommendations = recommendations.await()
                                updateSuccess(movieId) {
                                    it.copy(recommendations = loadedRecommendations.withLibraryState(libraryById))
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

        /**
         * Reloads only the credits section, keeping the main film and the recommendations
         * untouched. An initial load or retry already in flight is left as is.
         */
        fun retryCredits() {
            val state = _uiState.value as? MovieDetailUiState.Success ?: return
            if (state.credits == MovieCreditsUiState.Loading || creditsRetryJob?.isActive == true) return
            val movieId = state.movie.id
            creditsRetryJob =
                viewModelScope.launch {
                    updateSuccess(movieId) { it.copy(credits = MovieCreditsUiState.Loading) }
                    val loadedCredits = loadCredits(movieId)
                    updateSuccess(movieId) { it.copy(credits = loadedCredits) }
                }
        }

        /** Reloads only recommendations, keeping any initial load or retry already in flight. */
        fun retryRecommendations() {
            val state = _uiState.value as? MovieDetailUiState.Success ?: return
            if (state.recommendations == MovieRecommendationsUiState.Loading || recommendationsRetryJob?.isActive == true) return
            val movieId = state.movie.id
            recommendationsRetryJob =
                viewModelScope.launch {
                    updateSuccess(movieId) { it.copy(recommendations = MovieRecommendationsUiState.Loading) }
                    val loadedRecommendations = loadRecommendations(movieId)
                    updateSuccess(movieId) {
                        it.copy(recommendations = loadedRecommendations.withLibraryState(libraryById))
                    }
                }
        }

        /**
         * Restarts only the library observer. Library writes stay disabled until the restarted
         * observer produces a valid snapshot.
         */
        fun retryLibraryObservation() {
            val movieId = currentMovieId() ?: return
            updateSuccess(movieId) { it.copy(libraryObservation = LibraryObservationUiState.Loading) }
            startObservingLibrary(movieId)
        }

        fun toggleFavorite() = toggleLibraryFlag(LibraryFlag.FAVORITE)

        fun toggleWatchlist() = toggleLibraryFlag(LibraryFlag.WATCHLIST)

        fun toggleWatched() = toggleLibraryFlag(LibraryFlag.WATCHED)

        /** Clears the pending library error once its message has finished being displayed. */
        fun acknowledgeLibraryActionError() {
            _libraryActionErrorPending.value = false
        }

        private fun toggleLibraryFlag(flag: LibraryFlag) {
            viewModelScope.launch {
                libraryMutex.withLock {
                    val state = _uiState.value as? MovieDetailUiState.Success ?: return@withLock
                    // The persisted flags are only safe to toggle once the observer loaded them.
                    if (state.libraryObservation != LibraryObservationUiState.Ready) return@withLock
                    val previous = state.movie
                    val value = !flag.isSet(previous)
                    _uiState.value = state.copy(movie = flag.apply(previous, value), isLibraryActionPending = true)

                    try {
                        libraryRepository.setLibraryFlag(previous, flag, value)
                        setLibraryActionPending(previous.id, false)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _libraryActionErrorPending.value = true
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

        private suspend fun loadCredits(movieId: Int): MovieCreditsUiState =
            try {
                MovieCreditsUiState.Success(repository.getMovieCredits(movieId))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                MovieCreditsUiState.Error
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
         *
         * A failed snapshot keeps the last known flags and reports the sync error instead of
         * applying the empty map, so a transient read failure never clears the UI.
         */
        private fun startObservingLibrary(movieId: Int) {
            libraryJob?.cancel()
            libraryJob =
                viewModelScope.launch {
                    try {
                        libraryRepository.getLibraryMovies().collect { localMovies ->
                            val localById = localMovies.associateBy(Movie::id)
                            libraryById = localById
                            updateSuccess(movieId) {
                                it.withLibraryState(localById).copy(libraryObservation = LibraryObservationUiState.Ready)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        updateSuccess(movieId) { it.copy(libraryObservation = LibraryObservationUiState.Error) }
                    }
                }
        }

        private fun currentMovieId(): Int? {
            val state = _uiState.value as? MovieDetailUiState.Success ?: return null
            return state.movie.id
        }

        /** Cancels the load, the library observer and any in-flight section retry. */
        private fun cancelSectionWork() {
            loadJob?.cancel()
            libraryJob?.cancel()
            creditsRetryJob?.cancel()
            recommendationsRetryJob?.cancel()
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
 * [localById] is null while no valid snapshot has been observed yet, in which case the
 * repository flags are kept instead of being cleared from an unknown library.
 *
 * This deliberately differs from [com.benjamin.moviehub.data.mapper.withLocalFlags],
 * which keeps the remote flags for a not-yet-cached movie in network feeds.
 */
private fun MovieRecommendationsUiState.withLibraryState(localById: Map<Int, Movie>?): MovieRecommendationsUiState =
    if (localById == null) {
        this
    } else {
        (this as? MovieRecommendationsUiState.Success)
            ?.let { success ->
                MovieRecommendationsUiState.Success(success.movies.map { it.withLibraryState(localById[it.id]) }.toImmutableList())
            } ?: this
    }

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
        val credits: MovieCreditsUiState,
        val recommendations: MovieRecommendationsUiState = MovieRecommendationsUiState.Loading,
        val isLibraryActionPending: Boolean = false,
        val libraryObservation: LibraryObservationUiState = LibraryObservationUiState.Loading,
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
