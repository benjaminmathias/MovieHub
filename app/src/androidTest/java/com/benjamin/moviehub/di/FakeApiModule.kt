package com.benjamin.moviehub.di

import com.benjamin.moviehub.data.remote.FakeMovieApiService
import com.benjamin.moviehub.data.remote.MovieApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [RemoteModule::class],
)
object FakeApiModule {
    @Provides
    @Singleton
    fun provideFakeMovieApiService(): FakeMovieApiService = FakeMovieApiService()

    @Provides
    @Singleton
    fun provideMovieApiService(fake: FakeMovieApiService): MovieApiService = fake
}
