package com.example.geminispotifyapp.di

import android.content.Context
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService
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
    fun provideSpotifyRepository(@ApplicationContext context: Context, spotifyUserApiService: SpotifyUserApiService, spotifyApiService: SpotifyApiService): SpotifyRepository {
        return SpotifyRepository.create(context, spotifyUserApiService, spotifyApiService)
    }
}