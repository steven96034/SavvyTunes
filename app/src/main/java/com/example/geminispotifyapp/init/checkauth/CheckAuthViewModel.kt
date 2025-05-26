package com.example.geminispotifyapp.init.checkauth

import androidx.lifecycle.ViewModel
import com.example.geminispotifyapp.SpotifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CheckAuthViewModel @Inject constructor(spotifyRepository: SpotifyRepository) : ViewModel() {
    private val _accessToken = MutableStateFlow(spotifyRepository.getAccessToken())
    val accessToken: StateFlow<String?> = _accessToken
}