package com.benjamin.moviehub

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.mapper.toEntity
import com.benjamin.moviehub.data.remote.MovieDto
import com.benjamin.moviehub.domain.model.GenreObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MovieMapperUnitTest {
    private fun createFakeEntity(
        id: Int = 1,
        isFavorite: Boolean = false,
    ) = MovieEntity(
        id = id,
        title = "Test Movie",
        overview = "Description",
        posterPath = "",
        backdropPath = "",
        voteAverage = 7.5,
        releaseDate = "2024-01-01",
        isFavorite = isFavorite,
    )

    private fun createFakeDto(
        id: Int = 1,
        posterPath: String? = null,
    ) = MovieDto(
        id = id,
        title = "Test Movie",
        description = "Description",
        posterPath = posterPath,
        backdropPath = "",
        voteAverage = 7.5,
        releaseDate = "2024-01-01",
    )

    @Test
    fun `Check image URL format`() {
        val dto = createFakeDto(posterPath = "/pic.jpg")
        val result = dto.toDomain()

        assertEquals("https://image.tmdb.org/t/p/w500/pic.jpg", result.posterPath)
    }

    @Test
    fun `Check favorite status is mapped correctly`() {
        val entity = createFakeEntity(isFavorite = true)
        val result = entity.toDomain()

        assertEquals(true, result.isFavorite)
    }

    @Test
    fun `Check null poster path is handled correctly`() {
        val dto = createFakeDto(posterPath = null)
        val result = dto.toDomain()

        assertEquals("", result.posterPath)
    }

    @Test
    fun `toEntity should handle null fields and use default empty strings`() {
        val dto = createFakeDto(posterPath = null)
        val entity = dto.toEntity(isFavorite = true)

        assertEquals(null, entity.posterPath)
        assertEquals(true, entity.isFavorite)
    }

    @Test
    fun `toEntity stores relative paths when dto contains a complete TMDB url`() {
        val dto = createFakeDto(posterPath = "https://image.tmdb.org/t/p/w500/pic.jpg")

        assertEquals("/pic.jpg", dto.toEntity().posterPath)
    }

    @Test
    fun `toDomain does not duplicate an already complete image url`() {
        val dto = createFakeDto(posterPath = "https://image.tmdb.org/t/p/w500/pic.jpg")

        assertEquals("https://image.tmdb.org/t/p/w500/pic.jpg", dto.toDomain().posterPath)
    }

    @Test
    fun `toDomain from Entity should keep all status flags intact`() {
        val entity = createFakeEntity(isFavorite = true)
        val domain = entity.toDomain()

        assertEquals(true, domain.isFavorite)
        assertEquals("https://www.themoviedb.org/movie/1", domain.webUrl)
    }

    @Test
    fun `toEntity should map page order correctly`() {
        val dto = createFakeDto(id = 500)
        val entity = dto.toEntity(pageOrder = 10)

        assertEquals(10, entity.pageOrder)
        assertEquals(500, entity.id)
    }

    @Test
    fun `toEntity should extract genre IDs from genre objects when genreIds is null`() {
        val detailDto =
            MovieDto(
                id = 1,
                title = "Test Movie",
                description = "Desc",
                posterPath = null,
                backdropPath = null,
                voteAverage = 8.0,
                releaseDate = "2025-01-01",
                genreIds = null,
                genres =
                    listOf(
                        GenreObject(id = 28, name = "Action"),
                        GenreObject(id = 12, name = "Aventure"),
                    ),
            )

        val entity = detailDto.toEntity()

        val expectedGenreIds = listOf(28, 12)
        assertEquals(expectedGenreIds, entity.genreIds)
    }

    @Test
    fun `toEntity should prioritize genreIds if available`() {
        val listDto =
            MovieDto(
                id = 1,
                title = "Test Movie",
                description = "Desc",
                posterPath = null,
                backdropPath = null,
                voteAverage = 8.0,
                releaseDate = "2025-01-01",
                genreIds = listOf(99, 100),
                genres = null,
            )

        val entity = listDto.toEntity()

        assertEquals(listOf(99, 100), entity.genreIds)
    }
}
