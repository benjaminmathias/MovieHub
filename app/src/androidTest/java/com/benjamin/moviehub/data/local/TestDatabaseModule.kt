package com.benjamin.moviehub.data.local

import android.content.Context
import androidx.room.Room
import com.benjamin.moviehub.di.DatabaseModule
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
object TestDatabaseModule {
    @Provides
    @Singleton
    fun provideInMemoryDb(@ApplicationContext context: Context): TestMovieDatabase {
        return Room.inMemoryDatabaseBuilder(context, TestMovieDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideMovieDao(db: TestMovieDatabase): MovieDao = db.movieDao()
}