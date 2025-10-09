package com.example.geminispotifyapp.di

import com.example.geminispotifyapp.TokenAuthenticator
import com.example.geminispotifyapp.ErrorHandlingInterceptor
import com.example.geminispotifyapp.TokenInterceptor
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetWorkModule {

    private const val API_BASE_URL = "https://api.spotify.com/"
    @Provides
    @Singleton
    fun provideSpotifyApiService(
        @UserOkHttpClient okHttpClient: OkHttpClient
    ): SpotifyUserApiService {
        return Retrofit.Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SpotifyUserApiService::class.java)
    }

    private const val ACCOUNT_BASE_URL = "https://accounts.spotify.com/"
    @Provides
    @Singleton
    fun provideSpotifyAccountApiService(
        @AuthOkHttpClient okHttpClient: OkHttpClient
    ): SpotifyApiService {
        return Retrofit.Builder()
            .baseUrl(ACCOUNT_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SpotifyApiService::class.java)
    }

    // Singleton OkHttpClient instance
    @UserOkHttpClient
    @Provides
    @Singleton
    fun provideUserOkHttpClient(
        tokenInterceptor: TokenInterceptor,
        authenticator: TokenAuthenticator,
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(tokenInterceptor)
            .authenticator(authenticator)
            .addInterceptor(errorHandlingInterceptor)
            .build()
    }

    @AuthOkHttpClient
    @Provides
    @Singleton
    fun provideAuthOkHttpClient(
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addInterceptor(errorHandlingInterceptor)
            .build()
    }

    @WeatherInfoGist
    @Provides
    @Singleton
    fun provideWeatherInfoGistOkHttpClient(
        errorHandlingInterceptor: ErrorHandlingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(errorHandlingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder().create()
    }
}