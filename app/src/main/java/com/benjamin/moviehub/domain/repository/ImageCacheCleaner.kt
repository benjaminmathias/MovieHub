package com.benjamin.moviehub.domain.repository

/** Clears cached movie artwork from memory and disk. */
interface ImageCacheCleaner {
    suspend fun clear()
}
