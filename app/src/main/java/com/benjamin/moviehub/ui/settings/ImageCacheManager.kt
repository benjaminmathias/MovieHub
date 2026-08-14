package com.benjamin.moviehub.ui.settings

import android.content.Context
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ImageCacheManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        @OptIn(ExperimentalCoilApi::class)
        fun clear() {
            context.imageLoader.memoryCache?.clear()
            context.imageLoader.diskCache?.clear()
        }
    }
