package com.benjamin.moviehub.util

import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.collections.immutable.persistentListOf

/** Minimal domain movie for tests that only care about its id and library flags. */
fun testMovie(
    id: Int,
    favorite: Boolean = false,
    watchlist: Boolean = false,
    watched: Boolean = false,
) = Movie(
    id = id,
    title = "Movie $id",
    overview = "",
    posterPath = null,
    backdropPath = null,
    voteAverage = 0.0,
    releaseDate = "",
    webUrl = null,
    isFavorite = favorite,
    isWatchlist = watchlist,
    isWatched = watched,
    genreIds = persistentListOf(),
    genres = persistentListOf(),
)

/** Minimal TMDB list item for paging and pagination tests. */
fun testMovieDto(id: Int) =
    MovieDto(
        id = id,
        title = "Movie $id",
        posterPath = null,
        backdropPath = null,
        voteAverage = 0.0,
    )
