package com.example.geminispotifyapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme
import com.example.geminispotifyapp.init.checkauth.CheckAuth
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Move the SpotifyRepository instance initialization to MyApplication.
//    private lateinit var spotifyRepository: SpotifyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        spotifyRepository = SpotifyRepository(this.applicationContext)
//        val spotifyRepository = this.app.spotifyRepository

        setContent {
            GeminiSpotifyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CheckAuth()
                }
            }
        }
    }
}

