package com.example.geminispotifyapp.features.findmusic

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.features.ContentScreen

@Composable
fun FindMusic(
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    ContentScreen("This is the Find Music screen.")
}