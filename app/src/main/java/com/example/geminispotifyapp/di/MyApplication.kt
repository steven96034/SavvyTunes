package com.example.geminispotifyapp.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// To maintain the singleton of Repository, we just put SpotifyRepository(applicationContext) in Application.
//val Context.app: MyApplication
//    get() = applicationContext as MyApplication // Safe typecast here.

// Use Hilt to manage the dependency in whole project.
@HiltAndroidApp
class MyApplication : Application() {
//    lateinit var spotifyRepository: SpotifyRepository
//        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize Repository when the application starts.
        // There may be memory leak if just using component activity context in MainActivity.
        // The lifecycle of Application Context is the same as the current application, also longer than Activity Context. (Using Hilt may be more better.)
//        spotifyRepository = SpotifyRepository.create(this)
    }
}