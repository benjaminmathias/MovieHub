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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow<MovieFavoriteListUiState>(MovieFavoriteListUiState.Loading)
        val uiState: StateFlow<MovieFavoriteListUiState> = _uiState.asStateFlow()

        private val refreshTrigger =
            MutableSharedFlow<Unit>(replay = 1).apply {
                tryEmit(Unit)
            }

        init {
            viewModelScope.launch {
                refreshTrigger
                    .flatMapLatest {
                        repository
                            .getFavoriteMovies()
                            .map { Result.success(it) }
                            .catch { error ->
                                if (error is CancellationException) throw error
                                emit(Result.failure(error))
                            }
                    }.collect { result ->
                        result
                            .onSuccess { movies ->
                                val emptyMsg =
                                    if (movies.isEmpty()) UiText.StringResource(R.string.no_favorite_added) else null
                                _uiState.value = MovieFavoriteListUiState.Success(movies, emptyMsg)
                            }.onFailure {
                                _uiState.value =
                                    MovieFavoriteListUiState.Error(UiText.StringResource(R.string.error_loading_movies))
                            }
                    }
            }
        }

        fun onToggleFavorite(movie: Movie) {
            viewModelScope.launch {
                repository.toggleFavorite(movie, !movie.isFavorite)
            }
        }

        fun refreshFavorite() {
            _uiState.value = MovieFavoriteListUiState.Loading
            refreshTrigger.tryEmit(Unit)
        }
    }
