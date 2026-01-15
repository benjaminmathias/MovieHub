package com.benjamin.moviehub.ui.navigation

sealed class Screen(val route: String) {
    object MovieList : Screen("movie_list")
    object MovieDetail : Screen("movie_detail/{movieId}") {
        fun createRoute(movieId: Int) = "movie_detail/$movieId"
    }
}