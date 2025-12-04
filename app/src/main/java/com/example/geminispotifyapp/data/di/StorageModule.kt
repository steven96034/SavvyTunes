package com.example.geminispotifyapp.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.example.geminispotifyapp.data.local.room.AppLocalDataSource
import com.example.geminispotifyapp.data.local.room.StructuredDatabase
import com.example.geminispotifyapp.data.local.room.TrackDao
import com.example.geminispotifyapp.data.local.room.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

//val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spotify_token_prefs")

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("spotify_token_prefs") }
        )
    }

    @Provides
    @Singleton
    fun provideAppLocalDataSource(trackDao: TrackDao, weatherDao: WeatherDao): AppLocalDataSource {
        return AppLocalDataSource(trackDao, weatherDao)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): StructuredDatabase {
        return Room.databaseBuilder(
            context,
            StructuredDatabase::class.java,
            "app_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideTrackDao(appDatabase: StructuredDatabase): TrackDao {
        return appDatabase.trackDao()
    }

    @Provides
    @Singleton
    fun provideWeatherDao(appDatabase: StructuredDatabase): WeatherDao {
        return appDatabase.weatherDao()
    }
}