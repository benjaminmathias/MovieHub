package com.benjamin.moviehub.domain.model

data class MovieCredits(
    val actors: List<Actor> = emptyList(),
    val director: String? = null,
)
