package com.benjamin.moviehub.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LibraryTab { WATCHLIST, FAVORITES, WATCHED }

sealed class LibraryUiState {
    data object Loading : LibraryUiState()
    data class Success(val movies: List<Movie>) : LibraryUiState()
    data class Error(val errorMessage: Int) : LibraryUiState()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(private val repository: MovieRepository) : ViewModel() {
    private val _actionErrors = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val actionErrors = _actionErrors.asSharedFlow()

    val uiState: StateFlow<LibraryUiState> =
        repository.getLibraryMovies().map { movies -> LibraryUiState.Success(movies) as LibraryUiState }
            .onStart { emit(LibraryUiState.Loading) }
            .catch { error ->
                if (error is CancellationException) throw error
                emit(LibraryUiState.Error(R.string.error_loading_movies))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState.Loading)

    fun onRemove(movie: Movie, tab: LibraryTab) {
        viewModelScope.launch {
            try {
                when (tab) {
                    LibraryTab.WATCHLIST -> repository.setWatchlist(movie, false)
                    LibraryTab.FAVORITES -> repository.setFavorite(movie, false)
                    LibraryTab.WATCHED -> repository.setWatched(movie, false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _actionErrors.tryEmit(Unit)
            }
        }
    }
}
