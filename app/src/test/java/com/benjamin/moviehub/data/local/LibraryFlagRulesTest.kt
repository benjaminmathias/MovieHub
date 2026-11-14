package com.benjamin.moviehub.data.local

import com.benjamin.moviehub.domain.model.LibraryFlag
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFlagRulesTest {
    private val movie =
        MovieEntity(
            id = 1,
            title = "Movie",
            overview = "",
            posterPath = null,
            backdropPath = null,
            voteAverage = 0.0,
            releaseDate = "2024-01-01",
        )

    private fun MovieEntity.flags() = Triple(isFavorite, isWatchlist, isWatched)

    @Test
    fun `favorite changes only the favorite flag`() {
        val start = movie.copy(isWatchlist = true)

        assertEquals(Triple(true, true, false), start.withLibraryFlag(LibraryFlag.FAVORITE, true).flags())
        assertEquals(Triple(false, true, false), start.withLibraryFlag(LibraryFlag.FAVORITE, false).flags())
    }

    @Test
    fun `setting watchlist or watched true clears the other`() {
        val favoriteWatched = movie.copy(isFavorite = true, isWatched = true)
        val favoriteWatchlist = movie.copy(isFavorite = true, isWatchlist = true)

        assertEquals(Triple(true, true, false), favoriteWatched.withLibraryFlag(LibraryFlag.WATCHLIST, true).flags())
        assertEquals(Triple(true, false, true), favoriteWatchlist.withLibraryFlag(LibraryFlag.WATCHED, true).flags())
    }

    @Test
    fun `setting watchlist or watched false keeps the other`() {
        val watched = movie.copy(isWatched = true)
        val watchlist = movie.copy(isWatchlist = true)

        assertEquals(Triple(false, false, true), watched.withLibraryFlag(LibraryFlag.WATCHLIST, false).flags())
        assertEquals(Triple(false, true, false), watchlist.withLibraryFlag(LibraryFlag.WATCHED, false).flags())
    }

    @Test
    fun `merge keeps local flags and runtime over network values`() {
        val incoming = movie.copy(title = "Fresh", isFavorite = false, runtimeMinutes = null)
        val local = movie.copy(title = "Stale", isFavorite = true, isWatchlist = true, runtimeMinutes = 120)

        val merged = incoming.mergedWithLocal(local)

        assertEquals("Fresh", merged.title)
        assertEquals(Triple(true, true, false), merged.flags())
        assertEquals(120, merged.runtimeMinutes)
    }

    @Test
    fun `merge prefers a new runtime and uses incoming flags without local row`() {
        val incoming = movie.copy(isFavorite = true, runtimeMinutes = 90)

        assertEquals(90, incoming.mergedWithLocal(movie.copy(runtimeMinutes = 120)).runtimeMinutes)
        assertEquals(Triple(true, false, false), incoming.mergedWithLocal(null).flags())
    }

    @Test
    fun `merge keeps only watched when a row is both watched and in the watchlist`() {
        val incoming = movie.copy(isWatchlist = true, isWatched = true)

        assertEquals(Triple(false, false, true), incoming.mergedWithLocal(null).flags())
    }
}
