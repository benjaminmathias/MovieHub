package com.benjamin.moviehub

import android.content.Context
import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.local.MovieEntity
import com.benjamin.moviehub.domain.model.Movie
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout

/** In-memory Room database used by the paging and persistence instrumented tests. */
internal fun inMemoryDatabase(): MovieDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MovieDatabase::class.java,
        ).build()

/** Differ that materializes [Movie] paging emissions for assertions. */
internal fun newMovieDiffer(): AsyncPagingDataDiffer<Movie> =
    AsyncPagingDataDiffer(
        diffCallback = MovieDiffCallback,
        updateCallback = NoOpListUpdateCallback,
        mainDispatcher = Dispatchers.Main,
    )

/** Waits until [condition] holds on the differ snapshot, then returns the visible items. */
internal suspend fun AsyncPagingDataDiffer<Movie>.awaitItems(condition: (List<Movie>) -> Boolean = { it.isNotEmpty() }): List<Movie> =
    withTimeout(10_000) {
        while (!condition(snapshot().filterNotNull())) delay(20)
        snapshot().filterNotNull()
    }

internal fun emptyPagingState(): PagingState<Int, MovieEntity> = PagingState(emptyList(), null, PagingConfig(pageSize = 1), 0)

internal fun pagingState(movie: MovieEntity): PagingState<Int, MovieEntity> =
    PagingState(
        pages = listOf(PagingSource.LoadResult.Page(data = listOf(movie), prevKey = null, nextKey = 2)),
        anchorPosition = null,
        config = PagingConfig(pageSize = 1),
        leadingPlaceholderCount = 0,
    )

internal fun movieEntity(
    id: Int,
    title: String = "Movie $id",
    isFavorite: Boolean = false,
    isWatchlist: Boolean = false,
    isWatched: Boolean = false,
    runtimeMinutes: Int? = null,
): MovieEntity =
    MovieEntity(
        id = id,
        title = title,
        overview = "Overview",
        posterPath = null,
        backdropPath = null,
        voteAverage = 7.0,
        releaseDate = "2020-01-01",
        isFavorite = isFavorite,
        isWatchlist = isWatchlist,
        isWatched = isWatched,
        runtimeMinutes = runtimeMinutes,
    )

private object MovieDiffCallback : DiffUtil.ItemCallback<Movie>() {
    override fun areItemsTheSame(
        oldItem: Movie,
        newItem: Movie,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: Movie,
        newItem: Movie,
    ): Boolean = oldItem == newItem
}

private object NoOpListUpdateCallback : ListUpdateCallback {
    override fun onInserted(
        position: Int,
        count: Int,
    ) = Unit

    override fun onRemoved(
        position: Int,
        count: Int,
    ) = Unit

    override fun onMoved(
        fromPosition: Int,
        toPosition: Int,
    ) = Unit

    override fun onChanged(
        position: Int,
        count: Int,
        payload: Any?,
    ) = Unit
}
