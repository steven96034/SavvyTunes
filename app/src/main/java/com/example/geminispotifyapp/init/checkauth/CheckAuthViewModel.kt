package com.example.geminispotifyapp.init.checkauth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CheckAuthViewModel @Inject constructor(spotifyRepositoryImpl: SpotifyRepositoryImpl, private val authManager: AuthManager) : ViewModel() {
    // Expose the StateFlow directly from the Repository
    val isAuthenticated: StateFlow<Boolean> = spotifyRepositoryImpl.currentAccessTokenFlow
        .map { accessToken ->
            // Transform the Access Token into a Boolean value, indicating whether logged in
            accessToken != null
        }.stateIn(
            scope = viewModelScope, // Ensure Flow is cancelled when ViewModel is cleared
            started = SharingStarted.WhileSubscribed(5000), // When there's subscribers, start collecting and stop after 5 seconds
            initialValue = false // Initial value, not logged in
        )

    fun startAuthentication() {
        viewModelScope.launch {
            authManager.startAuthentication()
        }
    }
}