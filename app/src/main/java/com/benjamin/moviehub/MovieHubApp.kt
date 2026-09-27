package com.benjamin.moviehub

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath

@HiltAndroidApp
class MovieHubApp :
    Application(),
    SingletonImageLoader.Factory {
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val diskCacheDirectory =
            context.cacheDir
                .resolve("image_cache")
                .absolutePath
                .toPath()

        return ImageLoader
            .Builder(context)
            .memoryCache {
                MemoryCache
                    .Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(diskCacheDirectory)
                    .maxSizeBytes(100L * 1024 * 1024)
                    .build()
            }.crossfade(true)
            .build()
    }
}
