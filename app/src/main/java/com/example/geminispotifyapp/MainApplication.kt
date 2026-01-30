package com.example.geminispotifyapp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.geminispotifyapp.core.utils.AppLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

// Use Hilt to manage the dependency in whole project.
@HiltAndroidApp
class MainApplication : Application() {
    @Inject
    lateinit var appLifecycleObserver: AppLifecycleObserver

    override fun onCreate() {
        super.onCreate()
        // Register the observer to update the last active time (whatever it's cold start or warm start)
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleObserver)
    }
}