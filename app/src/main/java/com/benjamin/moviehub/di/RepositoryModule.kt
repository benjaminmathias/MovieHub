package com.benjamin.moviehub.di

import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.data.repository.UserPreferenceRepositoryImpl
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferenceRepositoryImpl): UserPreferencesRepository
}
