package com.benjamin.moviehub.data.local

object MovieTestData {
    val popularMovie =
        MovieEntity(
            id = 1,
            title = "Avatar",
            overview = "Un film sur des gens bleus.",
            posterPath = "/path/avatar.jpg",
            backdropPath = "/path/backdrop.jpg",
            voteAverage = 7.8,
            releaseDate = "2009-12-18",
            isFavorite = false,
            isPopular = true, // SSOT : Ce film doit apparaître dans la liste populaire
        )

    val searchMovie =
        MovieEntity(
            id = 2,
            title = "No Country for Old Men",
            overview = "Un tueur avec une coupe au bol.",
            posterPath = "/path/nocountry.jpg",
            backdropPath = "/path/backdrop2.jpg",
            voteAverage = 8.1,
            releaseDate = "2007-11-09",
            isFavorite = true, // SSOT : Ce film est un favori mais n'est pas "populaire" au sens API
            isPopular = false,
        )
}
