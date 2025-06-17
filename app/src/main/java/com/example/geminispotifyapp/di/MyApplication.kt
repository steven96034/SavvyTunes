package com.example.geminispotifyapp.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp


// Use Hilt to manage the dependency in whole project.
@HiltAndroidApp
class MyApplication : Application()