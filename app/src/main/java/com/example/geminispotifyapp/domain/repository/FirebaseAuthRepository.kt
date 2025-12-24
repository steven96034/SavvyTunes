package com.example.geminispotifyapp.domain.repository

import com.example.geminispotifyapp.data.remote.model.WeeklyRecommendation

interface FirebaseAuthRepository {
    // This function will sync user data to Firestore after login
    suspend fun syncUserDataAfterLogin()
    // Use Google ID Token to log in Firebase
    suspend fun signInWithGoogle(idToken: String): Result<Boolean>
    // Update last active time in Firestore
    suspend fun updateLastActiveTime()
    // Get latest recommendation from Firestore
    suspend fun getLatestRecommendation(): Result<WeeklyRecommendation?>
}