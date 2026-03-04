package com.benjamin.moviehub.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel
    @Inject
    constructor(
        private val repository: MovieRepository,
    ) : ViewModel() {
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        private val _selectedGenres = MutableStateFlow<Set<String>>(emptySet())
        val selectedGenres = _selectedGenres.asStateFlow()

        private val refreshTrigger =
            MutableSharedFlow<Unit>(replay = 1).apply {
                tryEmit(Unit)
            }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val pagedMovies =
            _searchQuery
                .debounce(500L)
                .distinctUntilChanged()
                .combine(refreshTrigger) { query, _ -> query }
                .flatMapLatest { query ->
                    repository.getPagedMovies(query)
                }.cachedIn(viewModelScope)

        val uiState: StateFlow<MovieListUiState> =
            _searchQuery
                .map { query ->
                    MovieListUiState.Success(
                        pagedMovies = pagedMovies,
                        searchQuery = query,
                    )
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = MovieListUiState.Loading,
                )

        fun onSearchQueryChanged(newQuery: String) {
            _searchQuery.value = newQuery
        }

        fun retryGlobal() {
            refreshTrigger.tryEmit(Unit)
        }

        fun toggleGenre(genre: String) {
            val currentSet = _selectedGenres.value
            if (currentSet.contains(genre)) {
                _selectedGenres.value = currentSet - genre
            } else {
                _selectedGenres.value = currentSet + genre
            }
            println("Genres sélectionnés : ${_selectedGenres.value}")
        }
    }
