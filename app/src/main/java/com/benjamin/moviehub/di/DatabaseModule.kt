package com.benjamin.moviehub.di

import android.content.Context
import androidx.room.Room
import com.benjamin.moviehub.data.local.MIGRATION_1_2
import com.benjamin.moviehub.data.local.MIGRATION_2_3
import com.benjamin.moviehub.data.local.MIGRATION_3_4
import com.benjamin.moviehub.data.local.MIGRATION_4_5
import com.benjamin.moviehub.data.local.MIGRATION_5_6
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MovieDatabase =
        Room
            .databaseBuilder(
                context,
                MovieDatabase::class.java,
                "movie_hub_db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    @Singleton
    fun provideMovieDao(db: MovieDatabase): MovieDao = db.movieDao()
}
