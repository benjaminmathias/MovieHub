package com.benjamin.moviehub.ui.movie_list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)
    val uiState : StateFlow<MovieListUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    init {
        loadPopularMovies()
    }

    private fun loadPopularMovies() {
        viewModelScope.launch {
            repository.getPopularMovies()
                .catch { e ->
                    Log.e("MovieListViewModel", "Erreur de chargement", e) }
                .collect { movies ->
                    _uiState.value = MovieListUiState.Success(movies)
                }
           /* try {
                val movies = repository.getPopularMovies()
                _uiState.value = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                Log.e("MovieListViewModel", "Erreur de chargement", e)
                _uiState.value = MovieListUiState.Error("Impossible de récupérer les films. Vérifiez votre connexion")
            }*/
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
        searchJob?.cancel()

        if (newQuery.isBlank()) {
            loadPopularMovies()
        } else {
            searchJob = viewModelScope.launch {
                delay(500)
                searchMovies(newQuery)
            }
        }
    }

    private fun searchMovies(query: String) {
        viewModelScope.launch {
            try {
                _uiState.value = MovieListUiState.Loading
                val movies = repository.getSearchedMovies(query)
                _uiState.value = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                _uiState.value = MovieListUiState.Error("Aucun résultat trouvé")
            }
        }
    }
}