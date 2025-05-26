package com.example.geminispotifyapp.di

import android.content.Context
import com.example.geminispotifyapp.SpotifyRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideSpotifyRepository(@ApplicationContext context: Context): SpotifyRepository {
        return SpotifyRepository.create(context)
    }
}