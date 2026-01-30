package com.example.geminispotifyapp.domain.repository

import com.example.geminispotifyapp.data.remote.model.WeeklyRecommendation
import com.google.firebase.auth.AuthResult
import kotlinx.coroutines.flow.Flow

interface FirebaseAuthRepository {
    val lastUpdatedEverydayRecommendationDateFlow: Flow<String>
    // This function will sync user data to Firestore after login
    suspend fun syncUserDataAfterLogin()
    // Use Google ID Token to log in Firebase
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>
    // Update last active time in Firestore
    suspend fun updateLastActiveTimeAndTimeZone()
    // Get latest recommendation from Firestore
    suspend fun getLatestRecommendation(): Result<WeeklyRecommendation?>
    suspend fun signUpWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun loginWithEmail(email: String, password: String): Result<AuthResult>
    suspend fun signOut()
    suspend fun updateFcmToken(token: String)
}