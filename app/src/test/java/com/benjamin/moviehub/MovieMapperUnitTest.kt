package com.benjamin.moviehub

import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.mapper.toDomain
import com.benjamin.moviehub.data.remote.MovieDto
import org.junit.Assert.assertEquals
import org.junit.Test


class MovieMapperUnitTest {

    private fun createFakeEntity(
        id: Int = 1,
        isFavorite: Boolean = false
    ) = MovieEntity(
        id = id,
        title = "Test Movie",
        overview = "Description",
        posterPath = "",
        backdropPath = "",
        voteAverage = 7.5,
        releaseDate = "2024-01-01",
        isFavorite = isFavorite
    )

    private fun createFakeDto(
        id: Int = 1,
        posterPath: String? = null
    ) = MovieDto(
        id = id,
        title = "Test Movie",
        description = "Description",
        posterPath = posterPath,
        backdropPath = "",
        voteAverage = 7.5,
        releaseDate = "2024-01-01"
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

        assertEquals("https://image.tmdb.org/t/p/w500", result.posterPath)
    }
}
