package com.benjamin.moviehub.di

import com.benjamin.moviehub.data.cache.ImageCacheManager
import com.benjamin.moviehub.data.repository.LibraryRepositoryImpl
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.data.repository.UserPreferencesRepositoryImpl
import com.benjamin.moviehub.domain.repository.ImageCacheCleaner
import com.benjamin.moviehub.domain.repository.LibraryRepository
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
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindImageCacheCleaner(impl: ImageCacheManager): ImageCacheCleaner
}
