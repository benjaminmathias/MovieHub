package com.benjamin.moviehub.data.local

import java.util.Locale

object SearchQueryKey {
    fun normalize(query: String): String = query.trim().lowercase(Locale.ROOT)

    fun remoteKeyType(query: String): String = "SEARCH:${normalize(query)}"
}
