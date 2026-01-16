package com.example.geminispotifyapp.presentation.features.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.geminispotifyapp.MainActivity
import com.example.geminispotifyapp.core.auth.EmailAuthManager
import com.example.geminispotifyapp.core.utils.toast
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EmailCallbackActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var emailAuthManager: EmailAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val emailLink = intent.data.toString()

        if (firebaseAuth.isSignInWithEmailLink(emailLink)) {
            val email = emailAuthManager.getPendingEmail()

            if (email != null) {
                lifecycleScope.launch {
                    emailAuthManager.signInWithEmailLink(email, emailLink)
                        .onSuccess {
                            toast(this@EmailCallbackActivity, "Success! Your account has been created. Welcome aboard!", Toast.LENGTH_LONG)
                            emailAuthManager.clearPendingEmail()
                            navigateToMainActivity()
                        }
                        .onFailure { e ->
                            toast(this@EmailCallbackActivity, "Signup failed: ${e.localizedMessage}", Toast.LENGTH_LONG)
                            emailAuthManager.clearPendingEmail()
                            navigateToMainActivity()
                        }
                }
            } else {
                toast(this, "No pending email found. Please try signing up again.", Toast.LENGTH_LONG)
                navigateToMainActivity()
            }
        } else {
            toast(this, "Invalid email link.", Toast.LENGTH_LONG)
            navigateToMainActivity()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}