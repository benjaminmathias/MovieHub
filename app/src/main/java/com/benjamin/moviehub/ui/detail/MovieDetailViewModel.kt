package com.benjamin.moviehub.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.MovieCredits
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<MovieDetailUiState>(MovieDetailUiState.Loading)
        val uiState: StateFlow<MovieDetailUiState> = _uiState.asStateFlow()
        private val _favoriteActionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val favoriteActionErrors = _favoriteActionErrors.asSharedFlow()
        private var loadJob: Job? = null
        private val favoriteMutex = Mutex()

        fun loadMovieDetails(movieId: Int) {
            val currentState = _uiState.value
            if (currentState is MovieDetailUiState.Success && currentState.movie.id == movieId) {
                return
            }

            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                _uiState.value = MovieDetailUiState.Loading

                try {
                    // Throws on movie failure and cancels the secondary children automatically.
                    val creditsDeferred = async { loadCredits(movieId) }
                    val recommendationsDeferred = async { loadRecommendations(movieId) }
                    val movie = repository.getMovieDetails(movieId)

                    _uiState.value = MovieDetailUiState.Success(movie, MovieCredits())
                    updateCredits(movieId, creditsDeferred.await())
                    updateRecommendations(movieId, recommendationsDeferred.await())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.value =
                        MovieDetailUiState.Error(R.string.error_loading_movie_detail)
                }
            }
        }

        fun toggleFavorite() {
            viewModelScope.launch {
                favoriteMutex.withLock {
                    val currentState = _uiState.value as? MovieDetailUiState.Success ?: return@withLock
                    val requestedMovie = currentState.movie
                    val newStatus = !requestedMovie.isFavorite

                    _uiState.value = currentState.copy(movie = requestedMovie.copy(isFavorite = newStatus))

                    try {
                        repository.toggleFavorite(requestedMovie, newStatus)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _favoriteActionErrors.tryEmit(Unit)
                        rollbackFavorite(requestedMovie.id, newStatus, currentState)
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

        private fun updateCredits(
            movieId: Int,
            credits: MovieCredits,
        ) {
            val latest = _uiState.value as? MovieDetailUiState.Success ?: return
            if (latest.movie.id == movieId) {
                _uiState.value = latest.copy(credits = credits)
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

        private fun updateRecommendations(
            movieId: Int,
            recommendations: MovieRecommendationsUiState,
        ) {
            val latest = _uiState.value as? MovieDetailUiState.Success ?: return
            if (latest.movie.id == movieId) {
                _uiState.value = latest.copy(recommendations = recommendations)
            }
        }

        private fun rollbackFavorite(
            movieId: Int,
            attemptedStatus: Boolean,
            previousState: MovieDetailUiState.Success,
        ) {
            val latest = _uiState.value as? MovieDetailUiState.Success ?: return
            if (latest.movie.id == movieId && latest.movie.isFavorite == attemptedStatus) {
                _uiState.value = previousState
            }
        }
    }
