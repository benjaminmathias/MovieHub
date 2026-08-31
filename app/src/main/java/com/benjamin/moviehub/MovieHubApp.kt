package com.benjamin.moviehub

import android.app.Application
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.benjamin.moviehub.data.worker.SyncMoviesWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import java.util.concurrent.TimeUnit

private const val SYNC_WORK_NAME = "daily_movie_sync"
private const val SYNC_WORK_MIGRATION_KEY = "sync_worker_data_package"

@HiltAndroidApp
class MovieHubApp :
    Application(),
    ImageLoaderFactory,
    Configuration.Provider {
    override fun onCreate() {
        super.onCreate()

        val constraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

        val syncWorkRequest =
            PeriodicWorkRequestBuilder<SyncMoviesWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()

        val workManager = WorkManager.getInstance(this)
        val migrationPreferences = getSharedPreferences("moviehub_migrations", MODE_PRIVATE)
        val workerPackageMigrated = migrationPreferences.getBoolean(SYNC_WORK_MIGRATION_KEY, false)
        val enqueueOperation = workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            if (workerPackageMigrated) {
                ExistingPeriodicWorkPolicy.KEEP
            } else {
                ExistingPeriodicWorkPolicy.UPDATE
            },
            syncWorkRequest,
        )

        if (!workerPackageMigrated) {
            enqueueOperation.result.addListener(
                {
                    try {
                        enqueueOperation.result.get()
                        migrationPreferences.edit().putBoolean(SYNC_WORK_MIGRATION_KEY, true).apply()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                    } catch (e: Exception) {
                        Log.w("MovieHubApp", "Impossible de migrer le worker de synchronisation", e)
                    }
                },
                ContextCompat.getMainExecutor(this),
            )
        }
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader
            .Builder(this)
            .memoryCache {
                MemoryCache
                    .Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }.diskCache {
                DiskCache
                    .Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }.crossfade(true)
            .build()

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
