package com.benjamin.moviehub.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.domain.model.MovieCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalPagingApi::class)
class MovieRemoteMediatorTest {
    private val dao = mockk<MovieDao>()
    private val database = mockk<MovieDatabase>()
    private val api = mockk<MovieApiService>()

    private fun mediator(): MovieRemoteMediator {
        every { database.movieDao() } returns dao
        return MovieRemoteMediator(api, database, MovieCategory.POPULAR)
    }

    private fun state() = PagingState<Int, MovieEntity>(emptyList(), null, moviePagingConfig, 0)

    @Test
    fun `append key read failure becomes a retryable paging error`() =
        runTest {
            val failure = IllegalStateException("key read failed")
            coEvery { dao.getRemoteKey(MovieCategory.POPULAR.key) } throws failure

            val result = mediator().load(LoadType.APPEND, state())

            assertTrue(result is RemoteMediator.MediatorResult.Error)
            assertSame(failure, (result as RemoteMediator.MediatorResult.Error).throwable)
            coVerify(exactly = 0) { api.getPopularMovies(any()) }
        }

    @Test
    fun `append key read cancellation is rethrown`() =
        runTest {
            val cancellation = CancellationException("cancelled")
            coEvery { dao.getRemoteKey(MovieCategory.POPULAR.key) } throws cancellation

            val thrown =
                try {
                    mediator().load(LoadType.APPEND, state())
                    null
                } catch (error: CancellationException) {
                    error
                }

            assertSame(cancellation, thrown)
        }

    @Test
    fun `append without a remote key ends pagination without a network request`() =
        runTest {
            coEvery { dao.getRemoteKey(MovieCategory.POPULAR.key) } returns null

            val result = mediator().load(LoadType.APPEND, state())

            assertTrue(result is RemoteMediator.MediatorResult.Success)
            assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
            coVerify(exactly = 0) { api.getPopularMovies(any()) }
        }
}
