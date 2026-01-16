package com.example.geminispotifyapp.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("email_auth_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val PENDING_EMAIL_KEY = "pending_email"
    }

    fun savePendingEmail(email: String) {
        prefs.edit().putString(PENDING_EMAIL_KEY, email).apply()
    }

    fun getPendingEmail(): String? {
        return prefs.getString(PENDING_EMAIL_KEY, null)
    }

    fun clearPendingEmail() {
        prefs.edit().remove(PENDING_EMAIL_KEY).apply()
    }

    suspend fun signInWithEmailLink(email: String, emailLink: String): Result<Unit> {
        return try {
            firebaseAuth.signInWithEmailLink(email, emailLink).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
