package com.example.geminispotifyapp.data.repository

import android.util.Log
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging
) : FirebaseAuthRepository {

    override suspend fun syncUserDataAfterLogin() {
        val user = auth.currentUser ?: throw Exception("User not logged in")

        try {
            // 1. Get FCM Token (non-blocking)
            val fcmToken = messaging.token.await()

            // 2. Prepare User Data
            // If displayName is empty, use email as displayName or set to "Unknown User"
            val name = user.displayName ?: user.email?.substringBefore("@") ?: "User"

            val userData = hashMapOf(
                "uid" to user.uid,
                "email" to (user.email ?: ""),
                "displayName" to name,
                "fcmToken" to fcmToken,
                "lastActiveAt" to com.google.firebase.Timestamp.now(),
            )

            // 3. Write to Firestore (use merge to avoid overwriting existing data)
            firestore.collection("users")
                .document(user.uid)
                .set(userData, SetOptions.merge())
                .await()

            Log.d("FirebaseAuthRepository", "User data synced successfully")

        } catch (e: Exception) {
            e.printStackTrace()
            // Handle error, e.g., FCM token retrieval failed or network issue
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Boolean> {
        return try {
            // 1. Convert Google ID Token to Firebase Credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)

            // 2. Log in with Firebase
            auth.signInWithCredential(credential).await()

            // 3. After successful login, execute the logic to sync user data (reuse the same code)
            syncUserDataAfterLogin()

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}