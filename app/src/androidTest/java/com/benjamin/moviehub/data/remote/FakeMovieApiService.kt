package com.benjamin.moviehub.data.remote

import java.io.IOException

/**
 * Fake déterministe de [MovieApiService], partagé entre les tests instrumentés
 * (UI et médiateurs de pagination). Aucun appel réseau réel n'est effectué.
 *
 * Les données sont configurables au constructeur ; les valeurs par défaut
 * suffisent aux tests de navigation et de recherche sans configuration.
 *
 * [failRequests] simule une panne réseau pour vérifier que le cache Room reste utilisable.
 */
class FakeMovieApiService(
    private val popularPages: Map<Int, List<MovieDto>> = defaultPopularPages,
    private val searchPages: Map<String, Map<Int, List<MovieDto>>> = defaultSearchPages,
    private val details: Map<Int, MovieDto> = emptyMap(),
    private val credits: Map<Int, MovieCreditsDto> = emptyMap(),
    private val recommendations: Map<Int, List<MovieDto>> = emptyMap(),
    private val nowPlayingPages: Map<Int, List<MovieDto>> = defaultNowPlayingPages,
    private val upcomingPages: Map<Int, List<MovieDto>> = defaultUpcomingPages,
    private val topRatedPages: Map<Int, List<MovieDto>> = defaultTopRatedPages,
    private val failRequests: Boolean = false,
) : MovieApiService {
    val popularPagesRequested = mutableListOf<Int>()
    val searchPagesRequested = mutableListOf<Int>()
    val nowPlayingPagesRequested = mutableListOf<Int>()
    val upcomingPagesRequested = mutableListOf<Int>()
    val topRatedPagesRequested = mutableListOf<Int>()
    val discoverPagesRequested = mutableListOf<Int>()

    override suspend fun getPopularMovies(page: Int): MovieResponse {
        popularPagesRequested += page
        failIfRequested()
        return MovieResponse(popularPages[page].orEmpty())
    }

    override suspend fun getNowPlayingMovies(page: Int): MovieResponse {
        nowPlayingPagesRequested += page
        failIfRequested()
        return MovieResponse(nowPlayingPages[page].orEmpty())
    }

    override suspend fun getUpcomingMovies(page: Int): MovieResponse {
        upcomingPagesRequested += page
        failIfRequested()
        return MovieResponse(upcomingPages[page].orEmpty())
    }

    override suspend fun getTopRatedMovies(page: Int): MovieResponse {
        topRatedPagesRequested += page
        failIfRequested()
        return MovieResponse(topRatedPages[page].orEmpty())
    }

    override suspend fun discoverMovies(
        genreId: Int?,
        releaseYear: Int?,
        minimumVoteAverage: Double?,
        sortBy: String,
        page: Int,
    ): MovieResponse {
        discoverPagesRequested += page
        failIfRequested()
        return MovieResponse(popularPages[page].orEmpty())
    }

    override suspend fun getMovieGenres(): MovieGenresResponse =
        MovieGenresResponse(
            genres =
                listOf(
                    GenreDto(id = 28, name = "Action"),
                    GenreDto(id = 18, name = "Drame"),
                ),
        )

    override suspend fun searchMovies(
        query: String,
        page: Int,
    ): MovieResponse {
        searchPagesRequested += page
        failIfRequested()
        val normalized = query.trim().lowercase()
        val pages = searchPages[normalized] ?: popularPages
        return MovieResponse(pages[page].orEmpty())
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDto = details[movieId] ?: movieDto(movieId)

    override suspend fun getMovieCredits(movieId: Int): MovieCreditsDto = credits[movieId] ?: MovieCreditsDto()

    override suspend fun getMovieRecommendations(movieId: Int): MovieResponse =
        MovieResponse(recommendations[movieId].orEmpty())

    private fun failIfRequested() {
        if (failRequests) throw IOException("Fake network failure")
    }

    companion object {
        val defaultPopularPages: Map<Int, List<MovieDto>> =
            mapOf(1 to (1..5).map { movieDto(id = it, title = "Film Populaire $it") })

        val defaultNowPlayingPages: Map<Int, List<MovieDto>> =
            mapOf(1 to (1..5).map { movieDto(id = 100 + it, title = "Film En Salle $it") })

        val defaultUpcomingPages: Map<Int, List<MovieDto>> =
            mapOf(1 to (1..5).map { movieDto(id = 200 + it, title = "Film Prochainement $it") })

        val defaultTopRatedPages: Map<Int, List<MovieDto>> =
            mapOf(1 to (1..5).map { movieDto(id = 300 + it, title = "Film Mieux Note $it") })

        val defaultSearchPages: Map<String, Map<Int, List<MovieDto>>> =
            mapOf("interstellar" to mapOf(1 to listOf(movieDto(id = 157336, title = "Interstellar"))))
    }
}

fun movieDto(
    id: Int,
    title: String = "Movie $id",
): MovieDto =
    MovieDto(
        id = id,
        title = title,
        description = "Overview",
        posterPath = null,
        backdropPath = null,
        voteAverage = 7.0,
        releaseDate = "2020-01-01",
        genreIds = emptyList(),
    )
