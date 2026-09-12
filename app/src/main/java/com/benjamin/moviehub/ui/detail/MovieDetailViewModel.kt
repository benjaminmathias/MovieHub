package com.benjamin.moviehub.ui.detail

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
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

        fun loadMovieDetails(movieId: Int) {
            val currentState = _uiState.value
            if (currentState is MovieDetailUiState.Success && currentState.movie.id == movieId) {
                return
            }

            loadJob?.cancel()
            libraryJob?.cancel()
            loadJob = viewModelScope.launch {
                _uiState.value = MovieDetailUiState.Loading

                try {
                    coroutineScope {
                        var latestLibraryById = emptyMap<Int, com.benjamin.moviehub.domain.model.Movie>()
                        val creditsDeferred = async { loadCredits(movieId) }
                        val recommendationsDeferred = async { loadRecommendations(movieId) }
                        // A main failure throws out of the scope and cancels both async children.
                        val movie = repository.getMovieDetails(movieId)

                        _uiState.value = MovieDetailUiState.Success(movie, MovieCredits())
                        libraryJob = viewModelScope.launch {
                            repository.getLibraryMovies().collectLatest { localMovies ->
                                val localById = localMovies.associateBy { it.id }
                                latestLibraryById = localById
                                updateSuccess(movieId) { latest ->
                                    val localMovie = localById[movieId]
                                    val updatedMovie =
                                        latest.movie.copy(
                                            isFavorite = localMovie?.isFavorite ?: false,
                                            isWatchlist = localMovie?.isWatchlist ?: false,
                                            isWatched = localMovie?.isWatched ?: false,
                                        )
                                    val updatedRecommendations =
                                        (latest.recommendations as? MovieRecommendationsUiState.Success)?.let { recommendations ->
                                            MovieRecommendationsUiState.Success(
                                                recommendations.movies.map { recommendation ->
                                                    localById[recommendation.id]?.let { local ->
                                                        recommendation.copy(
                                                            isFavorite = local.isFavorite,
                                                            isWatchlist = local.isWatchlist,
                                                            isWatched = local.isWatched,
                                                        )
                                                    } ?: recommendation.copy(
                                                        isFavorite = false,
                                                        isWatchlist = false,
                                                        isWatched = false,
                                                    )
                                                },
                                            )
                                        } ?: latest.recommendations
                                    latest.copy(movie = updatedMovie, recommendations = updatedRecommendations)
                                }
                            }
                        }
                        val credits = creditsDeferred.await()
                        updateSuccess(movieId) { it.copy(credits = credits) }
                        val recommendations = recommendationsDeferred.await()
                        val syncedRecommendations =
                            (recommendations as? MovieRecommendationsUiState.Success)?.let { success ->
                                MovieRecommendationsUiState.Success(
                                    success.movies.map { recommendation ->
                                        latestLibraryById[recommendation.id]?.let { local ->
                                            recommendation.copy(
                                                isFavorite = local.isFavorite,
                                                isWatchlist = local.isWatchlist,
                                                isWatched = local.isWatched,
                                            )
                                        } ?: recommendation
                                    },
                                )
                            } ?: recommendations
                        updateSuccess(movieId) { it.copy(recommendations = syncedRecommendations) }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value =
                        MovieDetailUiState.Error(R.string.error_loading_movie_detail)
                }
            }
        }

        fun toggleFavorite() {
            toggleLibraryFlag(
                current = Movie::isFavorite,
                persist = { movie, value -> repository.setFavorite(movie, value) },
                update = { movie, value -> movie.copy(isFavorite = value) },
                restore = { movie, previous, _ -> movie.copy(isFavorite = previous.isFavorite) },
            )
        }

        fun toggleWatchlist() {
            toggleLibraryFlag(
                current = Movie::isWatchlist,
                persist = { movie, value -> repository.setWatchlist(movie, value) },
                update = { movie, value -> movie.copy(isWatchlist = value, isWatched = if (value) false else movie.isWatched) },
                restore = { movie, previous, attempted ->
                    movie.copy(
                        isWatchlist = previous.isWatchlist,
                        isWatched = if (attempted) previous.isWatched else movie.isWatched,
                    )
                },
            )
        }

        fun toggleWatched() {
            toggleLibraryFlag(
                current = Movie::isWatched,
                persist = { movie, value -> repository.setWatched(movie, value) },
                update = { movie, value -> movie.copy(isWatched = value, isWatchlist = if (value) false else movie.isWatchlist) },
                restore = { movie, previous, attempted ->
                    movie.copy(
                        isWatched = previous.isWatched,
                        isWatchlist = if (attempted) previous.isWatchlist else movie.isWatchlist,
                    )
                },
            )
        }

        private fun toggleLibraryFlag(
            current: (Movie) -> Boolean,
            persist: suspend (Movie, Boolean) -> Unit,
            update: (Movie, Boolean) -> Movie,
            restore: (movie: Movie, previous: Movie, attempted: Boolean) -> Movie,
        ) {
            viewModelScope.launch {
                libraryMutex.withLock {
                    val currentState = _uiState.value as? MovieDetailUiState.Success ?: return@withLock
                    val previous = currentState.movie
                    val attempted = !current(previous)

                    _uiState.value = currentState.copy(movie = update(previous, attempted))

                    try {
                        persist(previous, attempted)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _libraryActionErrors.tryEmit(Unit)
                        rollbackLibrary(previous.id, previous, current, attempted, restore)
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

        private fun updateSuccess(
            movieId: Int,
            transform: (MovieDetailUiState.Success) -> MovieDetailUiState.Success,
        ) {
            val latest = _uiState.value as? MovieDetailUiState.Success ?: return
            if (latest.movie.id == movieId) {
                _uiState.value = transform(latest)
            }
        }

        private suspend fun loadRecommendations(movieId: Int): MovieRecommendationsUiState =
            try {
                val movies = repository.getMovieRecommendations(movieId)
                if (movies.isEmpty()) {
                    MovieRecommendationsUiState.Empty
                } else {
                    MovieRecommendationsUiState.Success(movies)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                MovieRecommendationsUiState.Error
            }

        private fun rollbackLibrary(
            movieId: Int,
            previous: Movie,
            current: (Movie) -> Boolean,
            attempted: Boolean,
            restore: (movie: Movie, previous: Movie, attempted: Boolean) -> Movie,
        ) {
            updateSuccess(movieId) { latest ->
                if (current(latest.movie) == attempted) {
                    latest.copy(movie = restore(latest.movie, previous, attempted))
                } else {
                    latest
                }
            }
        }
    }
