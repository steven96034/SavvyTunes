package com.example.geminispotifyapp.data.repository

import android.util.Log
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.model.WeeklyRecommendation
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class FirebaseAuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val messaging: FirebaseMessaging,
    private val appDatabase: AppDatabase
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

            // Sync user preferences from firestore to local database
            try {
                withContext(Dispatchers.IO) {
                    // Tasks.await() is safe to be called here
                    val snapshot = Tasks.await(
                        firestore.collection("users").document(user.uid)
                            .collection("preferences").document("music_settings").get()
                    )
                    val genre = snapshot.getString("genre")
                    val language = snapshot.getString("language")
                    val year = snapshot.getString("year")
                    val isRandom = snapshot.getBoolean("isRandom")

                    if (genre != null) {
                        appDatabase.saveGenreOfShowCaseSearch(genre)
                    }
                    if (language != null) {
                        appDatabase.saveLanguageOfShowCaseSearch(language)
                    }
                    if (year != null) {
                        appDatabase.saveYearOfShowCaseSearch(year)
                    }
                    if (isRandom != null) {
                        appDatabase.saveIsRandomYearOfShowCaseSelection(isRandom)
                    }
                    Log.d("FirebaseAuthRepository", "User prefs synced successfully")
                }
            } catch (e: Exception) {
                Log.e("FirebaseAuthRepository", "Firestore rescue failed when fetching refresh token.", e)
            }

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

    override suspend fun updateLastActiveTime() {
        // Update the lastActiveAt field in Firestore if the user is logged in
        val uid = auth.currentUser?.uid ?: return
        try {
            val updateData = mapOf(
                "lastActiveAt" to com.google.firebase.Timestamp.now()
            )
            firestore.collection("users")
                .document(uid)
                .set(updateData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            Log.d("FirebaseAuthRepository", "Error updating last active time: ${e.message}")
        }
    }

    override suspend fun getLatestRecommendation(): Result<WeeklyRecommendation?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))

        return try {
            // From recommendations collection, order by generatedAt in descending order, only take the first one (latest data)
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("recommendations")
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val recommendation = doc.toObject(WeeklyRecommendation::class.java)?.copy(id = doc.id)
                setLatestRecommendationOfDate(recommendation?.id)
                Result.success(recommendation)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override val lastUpdatedEverydayRecommendationDateFlow: Flow<String> = appDatabase.lastUpdatedEverydayRecommendationDateFlow
    private suspend fun setLatestRecommendationOfDate(date: String?) {
        if (date.isNullOrEmpty()) return
        Log.d("FirebaseAuthRepository", "setLatestRecommendationOfDate: $date")
        appDatabase.saveLastUpdatedEverydayRecommendationDate(date)
    }
}