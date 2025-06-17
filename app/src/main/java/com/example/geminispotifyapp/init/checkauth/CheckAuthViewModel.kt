package com.example.geminispotifyapp.init.checkauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckAuthViewModel @Inject constructor(spotifyRepository: SpotifyRepository, private val authManager: AuthManager) : ViewModel() {
    // Expose the StateFlow directly from the Repository
    val isAuthenticated: StateFlow<Boolean> = spotifyRepository.currentAccessTokenFlow
        .map { accessToken ->
            // Transform the Access Token into a Boolean value, indicating whether logged in
            accessToken != null
        }.stateIn(
            scope = viewModelScope, // Ensure Flow is cancelled when ViewModel is cleared
            started = SharingStarted.WhileSubscribed(5000), // When there's subscribers, start collecting and stop after 5 seconds
            initialValue = false // Initial value, not logged in
        )

//    val accessToken: StateFlow<String?> = spotifyRepository.currentAccessTokenFlow.stateIn(
//        scope = viewModelScope,
//        started = SharingStarted.WhileSubscribed(5000),
//        initialValue = null
//    )

    fun startAuthentication() {
        viewModelScope.launch {
            authManager.startAuthentication()
        }
    }
//
//    init {
//        spotifyRepository.getAccessTokenFlow().onEach { _accessToken.value = it }.launchIn(viewModelScope)
//    }
}