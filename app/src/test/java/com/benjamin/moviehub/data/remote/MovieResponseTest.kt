package com.benjamin.moviehub.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieResponseTest {
    private val pageSize = 20

    @Test
    fun `a page below an explicit total keeps pagination open`() {
        assertFalse(response(totalPages = 1000).isEndOfPagination(page = 499, pageSize = pageSize))
    }

    @Test
    fun `page 500 is terminal even when the reported total is beyond the cap`() {
        assertTrue(response(totalPages = 1000).isEndOfPagination(page = 500, pageSize = pageSize))
        assertTrue(response(totalPages = 1000).isEndOfPagination(page = 501, pageSize = pageSize))
        assertTrue(response(totalPages = null, movies = fullPage()).isEndOfPagination(page = 500, pageSize = pageSize))
    }

    @Test
    fun `an explicit total is terminal below the cap`() {
        assertTrue(response(totalPages = 499).isEndOfPagination(page = 499, pageSize = pageSize))
        assertFalse(response(totalPages = 500).isEndOfPagination(page = 499, pageSize = pageSize))
        assertTrue(response(totalPages = 500).isEndOfPagination(page = 500, pageSize = pageSize))
    }

    @Test
    fun `an omitted total falls back to an empty or short page`() {
        assertTrue(response(totalPages = null, movies = emptyList()).isEndOfPagination(page = 2, pageSize = pageSize))
        assertTrue(response(totalPages = null, movies = shortPage()).isEndOfPagination(page = 2, pageSize = pageSize))
        assertFalse(response(totalPages = null, movies = fullPage()).isEndOfPagination(page = 2, pageSize = pageSize))
    }

    private fun response(
        totalPages: Int?,
        movies: List<MovieDto> = emptyList(),
    ) = MovieResponse(movies = movies, totalPages = totalPages)

    private fun fullPage(): List<MovieDto> = List(pageSize) { movie(it) }

    private fun shortPage(): List<MovieDto> = listOf(movie(1))

    private fun movie(id: Int) = MovieDto(id = id, posterPath = null, backdropPath = null, voteAverage = 0.0)
}
