package com.benjamin.moviehub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import com.benjamin.moviehub.presentation.MovieListScreen
import com.benjamin.moviehub.presentation.MovieListViewModel
import com.benjamin.moviehub.ui.theme.MovieHubTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MovieHubTheme {
                val viewModel : MovieListViewModel = hiltViewModel()
                MovieListScreen(viewModel = viewModel)

            }
        }
    }
}