package com.benjamin.moviehub.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
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

sealed class LibraryUiState {
    data object Loading : LibraryUiState()

    data class Success(
        val movies: List<Movie>,
    ) : LibraryUiState()

    data class Error(
        val errorMessage: Int,
    ) : LibraryUiState()
}

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val retryTrigger = MutableStateFlow(0)
        private val _actionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val actionErrors = _actionErrors.asSharedFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<LibraryUiState> =
            retryTrigger
                .flatMapLatest {
                    repository
                        .getLibraryMovies()
                        .map { movies -> LibraryUiState.Success(movies) as LibraryUiState }
                        .onStart { emit(LibraryUiState.Loading) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(LibraryUiState.Error(R.string.error_loading_movies))
                        }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

        fun onRetry() {
            retryTrigger.update { it + 1 }
        }

        fun onRemove(
            movie: Movie,
            tab: LibraryTab,
        ) {
            viewModelScope.launch {
                try {
                    tab.removeFrom(repository, movie)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _actionErrors.tryEmit(Unit)
                }
            }
        }
    }
