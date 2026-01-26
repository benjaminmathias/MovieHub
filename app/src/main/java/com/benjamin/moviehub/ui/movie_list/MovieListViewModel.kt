package com.benjamin.moviehub.ui.movie_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.R
import com.benjamin.moviehub.core.util.UiText
import com.benjamin.moviehub.domain.model.Movie
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MovieListUiState> = _searchQuery
        .debounce(500L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            val flow: Flow<MovieListUiState> =
                if (query.isBlank()) {
                    repository.getPopularMovies().map { movies ->
                        val emptyMsg =
                            if (movies.isEmpty()) UiText.StringResource(R.string.no_movie_available) else null
                        MovieListUiState.Success(movies, emptyMsg)
                    }
                } else {
                    val favoriteIdsFlow = repository.getFavoriteMovies().map { list ->
                        list.map { it.id }.toSet()
                    }

                    val searchFlow = flow {
                        emit(repository.getSearchedMovies(query))
                    }

                    combine<List<Movie>, Set<Int>, MovieListUiState>(
                        searchFlow,
                        favoriteIdsFlow
                    ) { results, favoriteIds ->
                        val updatedMovies = results.map { movie ->
                            movie.copy(isFavorite = favoriteIds.contains(movie.id))
                        }
                        val emptyMsg = if (updatedMovies.isEmpty()) {
                            UiText.StringResource(R.string.empty_search_results, query)
                        } else null

                        MovieListUiState.Success(updatedMovies, emptyMsg)

                    }.onStart {
                        emit(MovieListUiState.Loading)
                    }
                }
            flow
        }
        .catch { e ->
            emit(MovieListUiState.Error(UiText.StringResource(R.string.error_loading_movies)))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MovieListUiState.Loading
        )

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun refreshMovies() {
        _searchQuery.value = _searchQuery.value
    }
}