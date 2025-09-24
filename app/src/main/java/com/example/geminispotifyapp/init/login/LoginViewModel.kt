package com.example.geminispotifyapp.init.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    spotifyRepository: SpotifyRepository,
    private val authManager: AuthManager
): ViewModel() {
    private val _navigateToUrlEvent = MutableSharedFlow<String>()
    val navigateToUrlEvent = _navigateToUrlEvent.asSharedFlow()
    val isAuthenticated: StateFlow<Boolean> = spotifyRepository.currentAccessTokenFlow
        .map { accessToken ->
            // Transform the Access Token into a Boolean value, indicating whether logged in
            !accessToken.isNullOrBlank()
        }.stateIn(
            scope = viewModelScope, // Ensure Flow is cancelled when ViewModel is cleared
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false // Initial value, not logged in
        )
    fun onLoginClicked() {
        viewModelScope.launch {
            val authUrl = authManager.generateAuthorizationUrlAndSaveVerifier()
            _navigateToUrlEvent.emit(authUrl)
        }
    }
}