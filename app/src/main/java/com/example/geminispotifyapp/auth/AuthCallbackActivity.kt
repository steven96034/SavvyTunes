package com.example.geminispotifyapp.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geminispotifyapp.ui.MainActivity
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.utils.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class AuthCallbackActivity : AppCompatActivity() {

    @Inject lateinit var spotifyRepository: SpotifyRepository
    @Inject lateinit var authManager: AuthManager

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
        //Log.d(TAG, "Received code: $code, error: $error, state: $state")
        //Log.d(TAG, "Saved state: $savedState")

        lifecycleScope.launch {
            val savedState = authManager.getAuthState()


            if (state == null || state != savedState) {
                Log.w(
                    TAG,
                    "Received state does not match saved state, may exist hazard for CSRF attack"
                )
                // Might be under the risk of CSRF attack
                showErrorAndFinish("Might be under the risk of CSRF attack. Please attempt to login again or check your browser.")
                return@launch
            }


            when {
                error != null -> {
                    Log.e(TAG, "Spotify Authentication Error: $error")
                    showErrorAndFinish("Spotify authentication error: $error")
                    return@launch
                }

                code != null -> {
                    Log.d(TAG, "Successfully received Authorization Code: $code")
                    exchangeCodeForToken(code)
                }

                else -> {
                    Log.w(TAG, "Callback URI does not contain code or error")
                    showErrorAndFinish("Missing code or error in callback URI")
                    return@launch
                }
            }
        }
    }

    private suspend fun exchangeCodeForToken(code: String) {
        val codeVerifier = authManager.getCodeVerifier()
        if (codeVerifier == null) {
            Log.e(TAG, "Saved Code Verifier not found!")
            showErrorAndFinish("Code Verifier not found")
            return
        }
        Log.d(TAG, "Preparing to exchange Token... Code: $code, Verifier: $codeVerifier")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = spotifyRepository.getAccessTokenThruAuth(
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = AuthManager.REDIRECT_URI,
                    clientId = AuthManager.CLIENT_ID,
                    codeVerifier = codeVerifier
                )

                Log.d(TAG, "Successfully received Access Token: ${response.accessToken}")
                Log.d("RefreshToken", "Successfully received Refresh Token: ${response.refreshToken}")

                spotifyRepository.updateTokenResponse(response)
                Log.d(
                    TAG,
                    "Token saved to SharedPreferences. Expires at: ${Date(System.currentTimeMillis() + (response.expiresIn * 1000))}"
                )

                withContext(Dispatchers.Main) {
                    navigateToMainActivity()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error exchanging Token", e)
                withContext(Dispatchers.Main) {
                    showErrorAndFinish("Error exchanging token")
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