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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieListViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    // private val _uiState = MutableStateFlow<MovieListUiState>(MovieListUiState.Loading)

    var uiState by mutableStateOf<MovieListUiState>(MovieListUiState.Loading)
    private set

    init {
        loadMovies()
    }

    private fun loadMovies() {
        viewModelScope.launch {
            try {
                val movies = repository.getPopularMovies()
                uiState = MovieListUiState.Success(movies)
            } catch (e: Exception) {
                Log.e("MovieListViewModel", "Erreur de chargement", e)
                uiState = MovieListUiState.Error("Impossible de récupérer les films. Vérifiez votre connexion")
            }
        }
    }
}