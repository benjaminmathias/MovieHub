package com.benjamin.moviehub.core.util

private const val TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

fun normalizeImagePath(path: String?): String? {
    val value = path?.trim().orEmpty()
    if (value.isEmpty()) return null

    val tmdbPath = value.substringAfter("image.tmdb.org/t/p/", missingDelimiterValue = "")
    if (tmdbPath.isNotEmpty()) {
        return tmdbPath.substringAfter('/', missingDelimiterValue = "").takeIf { it.isNotEmpty() }?.let { "/$it" }
    }

    return value.takeIf { !it.startsWith("http://") && !it.startsWith("https://") }?.let {
        if (it.startsWith('/')) it else "/$it"
    } ?: value
}

fun toTmdbImageUrl(
    path: String?,
    size: String,
): String {
    val normalizedPath = normalizeImagePath(path) ?: return ""
    if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
        return normalizedPath
    }
    return "$TMDB_IMAGE_BASE_URL$size$normalizedPath"
}
