package com.benjamin.moviehub

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.ActorDto
import com.benjamin.moviehub.data.remote.CrewMemberDto
import com.benjamin.moviehub.data.remote.GenreDto
import com.benjamin.moviehub.data.remote.MovieCreditsDto
import com.benjamin.moviehub.data.remote.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieMapperUnitTest {
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
    fun `standalone dto maps to a complete Movie domain model`() {
        val dto = createFakeDto(posterPath = "/pic.jpg").copy(genreIds = listOf(18))

        val domain = dto.toDomain()

        assertEquals(1, domain.id)
        assertEquals("https://image.tmdb.org/t/p/w500/pic.jpg", domain.posterPath)
        assertEquals("https://www.themoviedb.org/movie/1", domain.webUrl)
        assertEquals(listOf("Drame"), domain.genres)
        assertEquals(false, domain.isFavorite)
    }

    @Test
    fun `toDomain from Entity should keep all status flags intact`() {
        val domain = createFakeEntity(isFavorite = true).toDomain()

        assertEquals(true, domain.isFavorite)
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
    fun `credits dto maps actors and the first director`() {
        val credits =
            MovieCreditsDto(
                cast = listOf(ActorDto(7, "Name", "Role", "/profile.jpg")),
                crew =
                    listOf(
                        CrewMemberDto("Writer", "Writer"),
                        CrewMemberDto("Director Name", "Director"),
                    ),
            )

        val result = credits.toDomain()

        assertEquals(1, result.actors.size)
        assertEquals("Director Name", result.director)
    }

    private fun createFakeEntity(isFavorite: Boolean = false) =
        MovieEntity(
            id = 1,
            title = "Test Movie",
            overview = "Description",
            posterPath = "",
            backdropPath = "",
            voteAverage = 7.5,
            releaseDate = "2024-01-01",
            isFavorite = isFavorite,
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
