package com.example.geminispotifyapp.di
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AuthOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UserOkHttpClient

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class WeatherInfoGist