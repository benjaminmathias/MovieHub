package com.benjamin.moviehub.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repository: LibraryRepository,
    ) : ViewModel() {
        private val retryTrigger = MutableStateFlow(0)

        /** An action failed and its message has not been shown yet. */
        private val _actionError = MutableStateFlow<LibraryActionErrorState>(LibraryActionErrorState.None)
        val actionError: StateFlow<LibraryActionErrorState> = _actionError.asStateFlow()

        @OptIn(ExperimentalCoroutinesApi::class)
        val uiState: StateFlow<LibraryUiState> =
            retryTrigger
                .flatMapLatest {
                    repository
                        .getLibraryMovies()
                        .map { movies -> LibraryUiState.Success(movies.toImmutableList()) as LibraryUiState }
                        .onStart { emit(LibraryUiState.Loading) }
                        .catch { error ->
                            if (error is CancellationException) throw error
                            emit(LibraryUiState.Error(LibraryLoadErrorCode.LOAD_MOVIES))
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
                    repository.setLibraryFlag(movie, tab.flag, false)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    _actionError.value = LibraryActionErrorState.Failure(LibraryActionErrorCode.REMOVE_MOVIE)
                }
            }
        }

        /** Clears the pending action error once its message has finished being displayed. */
        fun acknowledgeActionError() {
            _actionError.value = LibraryActionErrorState.None
        }
    }
