package com.benjamin.moviehub.core.util

/** Base URL of the TMDB image CDN; a sized image URL is `<base><size><path>`. */
internal const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

private const val TMDB_PATH_MARKER = "/t/p/"

/**
 * Rewrites a TMDB image URL to another [size] (e.g. `w342`) so callers request the
 * resolution they actually display. Null, blank, non-TMDB and malformed values are
 * handled without throwing.
 */
fun String?.tmdbImageAt(size: String): String? {
    val url = this?.trim().orEmpty()
    if (url.isEmpty()) return null

    val markerIndex = url.indexOf(TMDB_PATH_MARKER)
    if (markerIndex < 0) return url

    val sizeStart = markerIndex + TMDB_PATH_MARKER.length
    val pathStart = url.indexOf('/', sizeStart)
    if (pathStart < 0) return url

    val path = url.substring(pathStart + 1)
    return if (path.isEmpty()) url else "$TMDB_IMAGE_BASE_URL$size/$path"
}
