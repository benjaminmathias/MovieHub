package com.benjamin.moviehub.ui.settings

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImageCacheManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        @OptIn(ExperimentalCoilApi::class)
        suspend fun clear() =
            withContext(Dispatchers.IO) {
                context.imageLoader.memoryCache?.clear()
                context.imageLoader.diskCache?.clear()
            }
    }
