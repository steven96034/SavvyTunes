package com.example.geminispotifyapp.domain.repository

interface FirebaseAuthRepository {
    // This function will sync user data to Firestore after login
    suspend fun syncUserDataAfterLogin()

    // Only list sync data here, actual login method will be combined with Credential Manager or AuthUI
    // Use Google ID Token to log in Firebase
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>
}