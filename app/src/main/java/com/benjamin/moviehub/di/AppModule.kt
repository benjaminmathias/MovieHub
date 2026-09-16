package com.benjamin.moviehub.di

import android.content.Context
import androidx.room.Room
import com.benjamin.moviehub.BuildConfig
import com.benjamin.moviehub.data.connectivity.NetworkConnectivityObserver
import com.benjamin.moviehub.data.local.MIGRATION_1_2
import com.benjamin.moviehub.data.local.MIGRATION_2_3
import com.benjamin.moviehub.data.local.MIGRATION_3_4
import com.benjamin.moviehub.data.local.MIGRATION_4_5
import com.benjamin.moviehub.data.local.MIGRATION_5_6
import com.benjamin.moviehub.data.local.MovieDao
import com.benjamin.moviehub.data.local.MovieDatabase
import com.benjamin.moviehub.data.remote.MovieApiService
import com.benjamin.moviehub.data.repository.MovieRepositoryImpl
import com.benjamin.moviehub.data.repository.UserPreferencesRepositoryImpl
import com.benjamin.moviehub.domain.connectivity.ConnectivityObserver
import com.benjamin.moviehub.domain.repository.MovieRepository
import com.benjamin.moviehub.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(impl: UserPreferencesRepositoryImpl): UserPreferencesRepository
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ConnectivityModule {
    @Binds
    @Singleton
    abstract fun bindConnectivityObserver(networkConnectivityObserver: NetworkConnectivityObserver): ConnectivityObserver
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): MovieDatabase =
        Room
            .databaseBuilder(context, MovieDatabase::class.java, "movie_hub_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()

    @Provides
    @Singleton
    fun provideMovieDao(db: MovieDatabase): MovieDao = db.movieDao()
}

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor { chain ->
                val url =
                    chain
                        .request()
                        .url
                        .newBuilder()
                        .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                        .addQueryParameter("language", "fr-FR")
                        .build()
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .url(url)
                        .build(),
                )
            }.connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideMovieApiService(retrofit: Retrofit): MovieApiService = retrofit.create(MovieApiService::class.java)
}
