package com.example.geminispotifyapp.init.checkauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckAuthViewModel @Inject constructor(spotifyRepository: SpotifyRepository, private val authManager: AuthManager) : ViewModel() {
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    fun startAuthentication() {
        viewModelScope.launch {
            authManager.startAuthentication()
        }
    }

    init {
        spotifyRepository.getAccessTokenFlow().onEach { _accessToken.value = it }.launchIn(viewModelScope)
    }
}