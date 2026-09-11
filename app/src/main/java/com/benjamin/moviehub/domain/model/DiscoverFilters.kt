package com.benjamin.moviehub.domain.model

enum class DiscoverSortOption(
    val queryValue: String,
) {
    POPULARITY("popularity.desc"),
    RATING("vote_average.desc"),
    RELEASE_DATE("primary_release_date.desc"),
}

data class DiscoverFilters(
    val genreId: Int? = null,
    val releaseYear: Int? = null,
    val minimumVoteAverage: Double? = null,
    val sort: DiscoverSortOption = DiscoverSortOption.POPULARITY,
)

data class MovieGenre(
    val id: Int,
    val name: String,
)
