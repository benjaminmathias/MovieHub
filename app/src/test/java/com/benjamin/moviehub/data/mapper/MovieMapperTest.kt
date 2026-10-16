package com.benjamin.moviehub.data.mapper

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.ActorDto
import com.benjamin.moviehub.data.remote.CrewMemberDto
import com.benjamin.moviehub.data.remote.GenreDto
import com.benjamin.moviehub.data.remote.MovieCreditsDto
import com.benjamin.moviehub.data.remote.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieMapperTest {
    @Test
    fun `actor dto maps fields and profile url`() {
        val actor = ActorDto(7, "Name", "Role", "/profile.jpg").toDomain()

        assertEquals(7, actor.id)
        assertEquals("Name", actor.name)
        assertEquals("Role", actor.character)
        assertEquals("https://image.tmdb.org/t/p/w185/profile.jpg", actor.profileUrl)
        assertEquals("", ActorDto(7, "Name", "Role", null).toDomain().profileUrl)
    }

    @Test
    fun `toEntity normalizes image paths and keeps missing ones null`() {
        val nullPoster = createFakeDto(posterPath = null).toEntity(isFavorite = true)

        assertEquals(null, nullPoster.posterPath)
        assertEquals(true, nullPoster.isFavorite)
        assertEquals(
            "/pic.jpg",
            createFakeDto(posterPath = "https://image.tmdb.org/t/p/w500/pic.jpg").toEntity().posterPath,
        )
    }

    @Test
    fun `entity mapping restores images, genres, and local flags`() {
        val domain =
            createFakeEntity(
                isFavorite = true,
                isWatchlist = true,
                isWatched = true,
                runtimeMinutes = 137,
                genreIds = listOf(28, 18, 999),
            ).toDomain(genreNames = mapOf(28 to "Action locale"))

        assertEquals(true, domain.isFavorite)
        assertEquals(true, domain.isWatchlist)
        assertEquals(true, domain.isWatched)
        assertEquals(137, domain.runtimeMinutes)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", domain.posterPath)
        assertEquals("https://image.tmdb.org/t/p/w780/backdrop.jpg", domain.backdropPath)
        assertEquals(listOf(28, 18, 999), domain.genreIds)
        assertEquals(listOf("Action locale", "Drame"), domain.genres)
        assertEquals("https://www.themoviedb.org/movie/1", domain.webUrl)
    }

    @Test
    fun `toEntity extracts genre ids from genre objects when genreIds is null`() {
        val detailDto =
            createFakeDto().copy(
                genreIds = null,
                genres =
                    listOf(
                        GenreDto(id = 28, name = "Action"),
                        GenreDto(id = 12, name = "Aventure"),
                    ),
            )

        assertEquals(listOf(28, 12), detailDto.toEntity().genreIds)
    }

    @Test
    fun `detail dto maps useful rating metadata`() {
        val dto = createFakeDto().copy(voteCount = 8673, runtimeMinutes = 169)

        val result = dto.toDomain(dto.toEntity().toDomain())

        assertEquals(8673, result.voteCount)
        assertEquals(169, result.runtimeMinutes)
    }

    @Test
    fun `credits keep the first fifteen actors and choose the first named director`() {
        val credits =
            MovieCreditsDto(
                cast = (0..15).map { id -> ActorDto(id, "Actor $id", "Role $id", "/profile-$id.jpg") },
                crew =
                    listOf(
                        CrewMemberDto("  ", "Director"),
                        CrewMemberDto("Writer", "Writer"),
                        CrewMemberDto(" Director Name ", "Director"),
                        CrewMemberDto("Later Director", "Director"),
                    ),
            )

        val result = credits.toDomain()

        assertEquals((0..14).toList(), result.actors.map { it.id })
        assertEquals("Director Name", result.director)
    }

    private fun createFakeEntity(
        isFavorite: Boolean = false,
        isWatchlist: Boolean = false,
        isWatched: Boolean = false,
        runtimeMinutes: Int? = null,
        genreIds: List<Int> = emptyList(),
    ) = MovieEntity(
        id = 1,
        title = "Test Movie",
        overview = "Description",
        posterPath = "/poster.jpg",
        backdropPath = "/backdrop.jpg",
        voteAverage = 7.5,
        releaseDate = "2024-01-01",
        isFavorite = isFavorite,
        isWatchlist = isWatchlist,
        isWatched = isWatched,
        runtimeMinutes = runtimeMinutes,
        genreIds = genreIds,
    )

    private fun createFakeDto(posterPath: String? = null) =
        MovieDto(
            id = 1,
            title = "Test Movie",
            description = "Description",
            posterPath = posterPath,
            backdropPath = "",
            voteAverage = 7.5,
            releaseDate = "2024-01-01",
        )
}
