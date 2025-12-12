package com.example.geminispotifyapp.presentation.features.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.auth.AuthManager
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    spotifyRepository: SpotifyRepository,
    private val authManager: AuthManager,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val auth: FirebaseAuth
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

    private val _isUserLoggedInFirebase = MutableStateFlow(isUserLoggedInFirebase())
    val isUserLoggedInFirebase: StateFlow<Boolean> = _isUserLoggedInFirebase
    // Check if the user has logged in
    fun isUserLoggedInFirebase(): Boolean {
        return auth.currentUser != null
    }

    // Simulate login (actual project needs Google Sign-In or FirebaseUI)
    // In order to easily test Firestore writing, just write a simple anonymous login or Email login
    fun performFirebaseLoginTest() {
        viewModelScope.launch {
            try {
                // If not logged in, perform anonymous login (or Email login)
                if (auth.currentUser == null) {
                    auth.signInAnonymously().await()
                    // or auth.signInWithEmailAndPassword("test@example.com", "password").await()
                }

                // After login successfully, sync data immediately
                firebaseAuthRepository.syncUserDataAfterLogin()

                _isUserLoggedInFirebase.value = isUserLoggedInFirebase()

                Log.d("LoginViewModel", "Log in with Firebase success(${_isUserLoggedInFirebase.value})！")

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Log in with Firebase failed: ${e.message}", e)
            }
        }
    }
}