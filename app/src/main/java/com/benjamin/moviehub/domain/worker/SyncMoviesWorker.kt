package com.benjamin.moviehub.domain.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.benjamin.moviehub.domain.repository.MovieRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

@HiltWorker
class SyncMoviesWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val repository: MovieRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result {
            Log.d("SyncWorker", "Début de la tâche de fond")
            return try {
                repository.syncPopularMoviesCache()
                Log.d("SyncWorker", "Tâche terminée avec succès")
                Result.success()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("SyncWorker", "Échec de la tâche", e)
                Result.retry()
            }
        }
    }
