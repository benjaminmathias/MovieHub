package com.benjamin.moviehub.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TmdbImagesTest {
    @Test
    fun `rewrites the size segment of a tmdb url`() {
        assertEquals(
            "https://image.tmdb.org/t/p/w342/poster.jpg",
            "https://image.tmdb.org/t/p/w500/poster.jpg".tmdbImageAt("w342"),
        )
    }

    @Test
    fun `keeps non tmdb urls untouched`() {
        assertEquals("https://example.com/poster.jpg", "https://example.com/poster.jpg".tmdbImageAt("w342"))
    }

    @Test
    fun `null and blank input map to null`() {
        assertEquals(null, (null as String?).tmdbImageAt("w342"))
        assertEquals(null, "   ".tmdbImageAt("w342"))
    }

    @Test
    fun `malformed tmdb urls are returned unchanged`() {
        assertEquals(
            "https://image.tmdb.org/t/p/poster.jpg",
            "https://image.tmdb.org/t/p/poster.jpg".tmdbImageAt("w342"),
        )
    }
}
