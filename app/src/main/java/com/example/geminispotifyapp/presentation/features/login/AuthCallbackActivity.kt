package com.example.geminispotifyapp.presentation.features.login

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geminispotifyapp.MainActivity
import com.example.geminispotifyapp.core.auth.AuthManager
import com.example.geminispotifyapp.core.utils.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthCallbackActivity : AppCompatActivity() {

    @Inject
    lateinit var authManager: AuthManager

    companion object {
        private const val TAG = "AuthCallbackActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate called")
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        Log.d(TAG, "handleIntent called with intent: $intent")
        val uri = intent?.data
        val expectedScheme = "geminispotifyapp"
        val expectedHost = "callback"

        if (uri == null || uri.scheme != expectedScheme || uri.host != expectedHost) {
            Log.w(TAG, "Received Intent Data is invalid or null")
            showErrorAndFinish("Invalid callback URI")
            return
        }

        val code = uri.getQueryParameter("code")
        val error = uri.getQueryParameter("error")
        val state = uri.getQueryParameter("state")

        lifecycleScope.launch {
            when {
                error != null -> {
                    Log.e(TAG, "Spotify Authentication Error: $error")
                    showErrorAndFinish("Spotify authentication error: $error")
                    return@launch
                }

                code != null -> {
                    Log.d(TAG, "Successfully received Authorization Code: $code")
                    val success = authManager.exchangeCodeForToken(code, state.orEmpty()) // Pass state to authManager
                    if (success) {
                        navigateToMainActivity()
                        finish()
                    } else {
                        showErrorAndFinish("Error exchanging token or state mismatch")
                    }
                }

                else -> {
                    Log.w(TAG, "Callback URI does not contain code or error")
                    showErrorAndFinish("Missing code or error in callback URI")
                    return@launch
                }
            }
        }
    }

    private fun navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity...")
        toast(this, "Spotify authorization successful")
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
    private fun showErrorAndFinish(errorMessage: String) {
        Log.e(TAG,"showErrorAndFinish：$errorMessage")
        // Displaying the error message in a dialog, then finishing the activity
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(errorMessage)
            .setPositiveButton("OK") { _, _ -> finish() }
            .setCancelable(false) // Prevent dismissing without acknowledgment
            .show()
    }
}