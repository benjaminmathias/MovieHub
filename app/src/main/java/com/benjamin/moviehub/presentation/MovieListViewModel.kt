package com.benjamin.moviehub.presentation

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benjamin.moviehub.domain.model.MovieListUiState
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var uiState by mutableStateOf<MovieListUiState>(MovieListUiState.Loading)
        private set

    private var searchJob: Job? = null

    init {
        loadPopularMovies()
    }

    private fun loadPopularMovies() {
        viewModelScope.launch {
            try {
                val movies = repository.getPopularMovies()
                uiState = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                Log.e("MovieListViewModel", "Erreur de chargement", e)
                uiState =
                    MovieListUiState.Error("Impossible de récupérer les films. Vérifiez votre connexion")
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery = newQuery
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
                uiState = MovieListUiState.Loading
                val movies = repository.getSearchedMovies(query)
                uiState = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                uiState = MovieListUiState.Error("Aucun résultat trouvé")
            }
        }
    }
}