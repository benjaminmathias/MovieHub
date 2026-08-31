package com.benjamin.moviehub.domain.worker

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.benjamin.moviehub.domain.repository.MovieRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class SyncMoviesWorkerTest {
    @Test
    fun `permanent http error fails without retry`() =
        runTest {
            mockkStatic(Log::class)
            every { Log.d(any(), any()) } returns 0
            every { Log.e(any(), any(), any()) } returns 0

            val repository = mockk<MovieRepository>()
            val response =
                Response.error<Any>(
                    401,
                    "unauthorized".toResponseBody("text/plain".toMediaType()),
                )
            coEvery { repository.syncPopularMoviesCache() } throws HttpException(response)

            val worker =
                SyncMoviesWorker(
                    mockk<Context>(relaxed = true),
                    mockk<WorkerParameters>(relaxed = true),
                    repository,
                )

            try {
                assertTrue(worker.doWork() is ListenableWorker.Result.Failure)
            } finally {
                unmockkStatic(Log::class)
            }
        }
}
