package com.example.geminispotifyapp.presentation.features.login

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.auth.AuthManager
import com.example.geminispotifyapp.core.auth.EmailAuthManager
import com.example.geminispotifyapp.core.utils.toast
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.ActionCodeSettings
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
    private val auth: FirebaseAuth,
    private val emailAuthManager: EmailAuthManager // Inject EmailAuthManager
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

    private val WEB_CLIENT_ID = "679870767238-6j8p7qjih4tsm5ui1e9ethlade16tldv.apps.googleusercontent.com"
    private val _isUserLoggedInFirebase = MutableStateFlow(auth.currentUser != null)
    val isUserLoggedInFirebase: StateFlow<Boolean> = _isUserLoggedInFirebase
    // Check if the user has logged in
    fun refreshAuthState() {
        _isUserLoggedInFirebase.value = auth.currentUser != null
    }

    fun handleAnonymousLogin() {
        viewModelScope.launch {
            try {
                auth.signInAnonymously()
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user\'s information
                            Log.d("LoginViewModel", "signInAnonymously:success")
                            refreshAuthState()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("LoginViewModel", "signInAnonymously:failure", task.exception)
                            refreshAuthState()
                        }
                    }
            } catch (e: Exception) {
                Log.d("LoginViewModel", "asGuestMode: ${e.message}")
            }
        }
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

                refreshAuthState()

                Log.d("LoginViewModel", "Log in with Firebase success(${_isUserLoggedInFirebase.value})！")

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Log in with Firebase failed: ${e.message}", e)
            }
        }
    }


    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun onLoginWithMailClick(email: String, password: String, context: Context) {
        if (email.isBlank() || password.isBlank()) {
            toast(context, "Please type in your email and password.")
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val result = firebaseAuthRepository.loginWithEmail(email, password)

            result.onSuccess {
                isLoading = false
                refreshAuthState()
            }.onFailure { e ->
                isLoading = false
                toast(context, "Log in Failed. Please try again.")
                Log.d("LoginViewModel", "onLoginWithMailClick: ${e.message}")
            }
        }
    }

    fun onSignUpWithMailClick(email: String, context: Context) {
        if (email.isBlank()) {
            toast(context, "Please type in your email.")
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            val actionCodeSettings = ActionCodeSettings.newBuilder()
                .setHandleCodeInApp(true)
                .setUrl("https://savvy-tunes.firebaseapp.com/finishSignUp") // Your deep link/website URL for email sign-in
                .setAndroidPackageName(
                    "com.example.geminispotifyapp", // Your Android package name
                    true, // Install if not already installed
                    null // Minimum app version (optional)
                )
                .build()

            auth.sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener { task ->
                    isLoading = false
                    if (task.isSuccessful) {
                        Log.d("LoginViewModel", "Email verification link sent to $email")
                        toast(context, "Verification link sent to $email. Please check your inbox to complete sign up.")
                        emailAuthManager.savePendingEmail(email)
                    } else {
                        toast(context, "Failed to send verification link. Please try again.")
                        Log.e("LoginViewModel", "Failed to send verification link: ${task.exception?.message}", task.exception)
                    }
                }
        }
    }

    fun handleGoogleLogin(context: Context) {
        viewModelScope.launch {
            try {
                // 1. Initialize Credential Manager
                val credentialManager = CredentialManager.create(context)

                // 2. Set Google ID option
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // whether to show only authorized accounts (usually set false for the first login)
                    .setServerClientId(WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false) // whether to automatically select an account (false for the first login)
                    .build()

                // 3. Create request
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 4. Send request (this will pop up a Google login window)
                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                // 5. Analyze the result to get the ID Token
                val credential = result.credential
                if (credential is CustomCredential) {
                    // Check if the type of this CustomCredential is Google ID
                    if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        try {
                            // Unpack the data from CustomCredential to GoogleIdTokenCredential object
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                            val idToken = googleIdTokenCredential.idToken

                            Log.d("LoginViewModel", "Successfully get Google ID Token: ${idToken.take(10)}...")


                            // 6. Call the repository to verify Firebase
                            firebaseAuthRepository.signInWithGoogle(idToken)
                                .onSuccess {
                                    Log.d("LoginViewModel", "Google Login & Sync Success!")
                                }
                                .onFailure {
                                    Log.d("LoginViewModel", "Firebase Auth Failed: ${it.message}")
                                }
                            refreshAuthState()

                        } catch (e: GoogleIdTokenParsingException) {
                            Log.d("LoginViewModel", "Cannot analyze Google ID Token: ${e.message}")
                        }
                    } else {
                        Log.d("LoginViewModel", "Receive unexpected CustomCredential type: ${credential.type}")
                    }
                }
            } catch (e: Exception) {
                // User canceled the login or an error occurred (e.g., No Credential)
                e.printStackTrace()
                Log.d("LoginViewModel", "Google Sign-In Error: ${e.message}")
            }
        }
    }
}