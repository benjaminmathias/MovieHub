package com.benjamin.moviehub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.benjamin.moviehub.ui.movie_detail.MovieDetailScreen
import com.benjamin.moviehub.ui.movie_detail.MovieDetailViewModel
import com.benjamin.moviehub.ui.movie_list.MovieListRoute

@Composable
fun MovieNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.MovieList.route // On commence par la liste
    ) {
        // Destination 1 : La Liste
        composable(route = Screen.MovieList.route) {
            //val viewModel: MovieListViewModel = hiltViewModel()
            MovieListRoute(
                //viewModel = viewModel,
                onMovieClick = { movieId ->
                    navController.navigate(Screen.MovieDetail.createRoute(movieId))
                }
            )
        }

        // Destination 2 : Le Détail
        composable(
            route = Screen.MovieDetail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.IntType })
        ) { backStackEntry ->
            val movieId = backStackEntry.arguments?.getInt("movieId") ?: 0
            val viewModel: MovieDetailViewModel = hiltViewModel()
            MovieDetailScreen(
                movieId = movieId,
                onBackClick = { navController.popBackStack() },
                viewModel
            )
        }
    }
}