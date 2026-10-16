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

internal fun inMemoryDatabase(): MovieDatabase =
    Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            MovieDatabase::class.java,
        ).build()

internal fun insertMalformedMovieForTest(
    database: MovieDatabase,
    movie: MovieEntity,
) {
    database.openHelper.writableDatabase.execSQL(
        """
        INSERT INTO movies (
            id, title, overview, posterPath, backdropPath, voteAverage, releaseDate,
            genreIds, isFavorite, isWatchlist, isWatched, runtimeMinutes
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(),
        arrayOf<Any?>(
            movie.id,
            movie.title,
            movie.overview,
            movie.posterPath,
            movie.backdropPath,
            movie.voteAverage,
            movie.releaseDate,
            movie.genreIds.joinToString(","),
            if (movie.isFavorite) 1 else 0,
            if (movie.isWatchlist) 1 else 0,
            if (movie.isWatched) 1 else 0,
            movie.runtimeMinutes,
        ),
    )
}

internal fun newMovieDiffer(): AsyncPagingDataDiffer<Movie> =
    AsyncPagingDataDiffer(
        diffCallback = MovieDiffCallback,
        updateCallback = NoOpListUpdateCallback,
        mainDispatcher = Dispatchers.Main,
    )

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
