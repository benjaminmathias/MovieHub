package com.benjamin.moviehub.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val retryTrigger = MutableStateFlow(0)
        private val _favoriteActionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val favoriteActionErrors = _favoriteActionErrors.asSharedFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<MovieFavoriteListUiState> =
            retryTrigger.flatMapLatest {
                repository.getFavoriteMovies()
                    .map { movies ->
                        val state: MovieFavoriteListUiState = MovieFavoriteListUiState.Success(
                            movies,
                            if (movies.isEmpty()) UiText.StringResource(R.string.no_favorite_added) else null,
                        )
                        state
                    }.onStart { emit(MovieFavoriteListUiState.Loading) }
                    .catch { error ->
                        if (error is CancellationException) throw error
                        emit(MovieFavoriteListUiState.Error(UiText.StringResource(R.string.error_loading_movies)))
                    }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                MovieFavoriteListUiState.Loading,
            )

        fun onRetry() {
            retryTrigger.update { it + 1 }
        }

        fun onToggleFavorite(movie: Movie) {
            viewModelScope.launch {
                try {
                    repository.toggleFavorite(movie, !movie.isFavorite)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _favoriteActionErrors.tryEmit(Unit)
                }
            }
        }
    }
