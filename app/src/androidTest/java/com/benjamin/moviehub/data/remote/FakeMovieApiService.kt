package com.benjamin.moviehub.data.remote

/**
 * Fake déterministe de [MovieApiService], partagé entre les tests instrumentés
 * (UI et médiateurs de pagination). Aucun appel réseau réel n'est effectué.
 *
 * Les données sont configurables au constructeur ; les valeurs par défaut
 * suffisent aux tests de navigation et de recherche sans configuration.
 */
class FakeMovieApiService(
    private val popularPages: Map<Int, List<MovieDto>> = defaultPopularPages,
    private val searchPages: Map<String, Map<Int, List<MovieDto>>> = defaultSearchPages,
    private val details: Map<Int, MovieDto> = emptyMap(),
    private val credits: Map<Int, MovieCreditsDto> = emptyMap(),
    private val recommendations: Map<Int, List<MovieDto>> = emptyMap(),
) : MovieApiService {
    val popularPagesRequested = mutableListOf<Int>()
    val searchPagesRequested = mutableListOf<Int>()

    override suspend fun getPopularMovies(page: Int): MovieResponse {
        popularPagesRequested += page
        return MovieResponse(popularPages[page].orEmpty())
    }

    override suspend fun searchMovies(
        query: String,
        page: Int,
    ): MovieResponse {
        searchPagesRequested += page
        val normalized = query.trim().lowercase()
        val pages = searchPages[normalized] ?: popularPages
        return MovieResponse(pages[page].orEmpty())
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDto = details[movieId] ?: movieDto(movieId)

    override suspend fun getMovieCredits(movieId: Int): MovieCreditsDto = credits[movieId] ?: MovieCreditsDto()

    override suspend fun getMovieRecommendations(movieId: Int): MovieResponse =
        MovieResponse(recommendations[movieId].orEmpty())

    companion object {
        val defaultPopularPages: Map<Int, List<MovieDto>> =
            mapOf(1 to (1..5).map { movieDto(id = it, title = "Film Populaire $it") })

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
