package com.example.geminispotifyapp.presentation.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.auth.AuthManager
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
    spotifyRepository: SpotifyRepository, // 保持為 private val
    private val authManager: AuthManager
): ViewModel() {
    private val _navigateToUrlEvent = MutableSharedFlow<String>()
    val navigateToUrlEvent = _navigateToUrlEvent.asSharedFlow()

    val isAuthenticated: StateFlow<Boolean> = spotifyRepository.currentAccessTokenFlow
        .map { accessToken ->
            !accessToken.isNullOrBlank()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun onLoginClicked() {
        viewModelScope.launch {
            val authUrl = authManager.generateAuthorizationUrlAndSaveVerifier()
            _navigateToUrlEvent.emit(authUrl)
        }
    }
}