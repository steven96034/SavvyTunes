package com.example.geminispotifyapp.features.findmusic

import androidx.lifecycle.ViewModel
import com.example.geminispotifyapp.SpotifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FindMusicViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository
): ViewModel() {
}