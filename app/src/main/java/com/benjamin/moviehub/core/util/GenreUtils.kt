package com.benjamin.moviehub.core.util

object GenreUtils {
    // Map Name to ID
    val genreMap = mapOf(
        "Action" to 28,
        "Aventure" to 12,
        "Animation" to 16,
        "Comédie" to 35,
        "Crime" to 80,
        "Documentaire" to 99,
        "Drame" to 18,
        "Familial" to 10751,
        "Fantastique" to 14,
        "Histoire" to 36,
        "Horreur" to 27,
        "Musique" to 10402,
        "Mystère" to 9648,
        "Romance" to 10749,
        "Science-Fiction" to 878,
        "Téléfilm" to 10770,
        "Thriller" to 53,
        "Guerre" to 10752,
        "Western" to 37
    )

    // Map ID to Name
    val idToNameMap = genreMap.entries.associate { (key, value) -> value to key }

    fun getGenreId(name: String): Int? {
        return genreMap[name]
    }
}