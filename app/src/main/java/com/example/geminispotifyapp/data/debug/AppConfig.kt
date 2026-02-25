package com.example.geminispotifyapp.data.debug

import com.example.geminispotifyapp.BuildConfig

object AppConfig {
    // For production, set this to false
    val isMockMode = BuildConfig.DEBUG && false
}