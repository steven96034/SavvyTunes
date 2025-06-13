package com.example.geminispotifyapp.auth

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.net.toUri
import com.example.geminispotifyapp.BuildConfig
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.utils.toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase
) {
    companion object {
        /**
         *  **Important**: Replace `YOUR_CLIENT_ID` with your actual Spotify Client ID from the Spotify Developer Dashboard.
         */
        const val CLIENT_ID = BuildConfig.SPOTIFY_WEB_API_KEY

        /**
         *  **Important**: The redirect URI must exactly match the one configured in the Spotify Developer Dashboard.
         *
         *  Also, ensure that the `Activity` in `AndroidManifest.xml` that handles this URI is configured with an appropriate `intent-filter`.
         */
        const val REDIRECT_URI = "geminispotifyapp://callback"

        /**
         * The list of scopes/permissions required for your application.
         *
         * [Spotify Scopes documentation](https://developer.spotify.com/documentation/general/guides/scopes/)
         */
        private val SCOPES = listOf(
            "playlist-read-private", // Read access to user's private playlists.
            "playlist-modify-private", // Write/delete access to the list of artists and other users that the user follows.
            "user-follow-read", // Read access to the list of artists and other users that the user follows.
            "user-library-modify", // Write/delete access to a user's "Your Music" library.
            "user-library-read", // Read access to a user's library.
            "user-top-read", // Read access to a user's top artists and tracks.
            "user-read-recently-played" // Read access to a user’s recently played tracks.
        )

        private const val AUTH_ENDPOINT = "https://accounts.spotify.com/authorize"
    }

    // --- PKCE Helper Functions ---

    /**
     * Generates a cryptographically secure random string for use as a PKCE Code Verifier.
     *
     * The recommended length for a code verifier is between 43 and 128 characters.
     *
     * @param length The desired length of the code verifier. Default is 64.
     * @return A randomly generated code verifier string.
     */
    private fun generateCodeVerifier(length: Int = 64): String {
        val possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.map { possibleChars[random.nextInt(possibleChars.length)] }.joinToString("")
    }

    /**
     * Computes the SHA-256 hash of the given string.
     *
     * @param input The string to hash.
     * @return The SHA-256 hash of the input string as a byte array.
     */
    private fun sha256(input: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * Performs Base64 URL-safe encoding on a byte array.
     *
     * @param input The byte array to encode.
     * @return The Base64 URL-safe encoded string.
     */
    private fun base64UrlEncode(input: ByteArray): String {
        return Base64.encodeToString(input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    /**
     * Generates a PKCE Code Challenge from a Code Verifier.
     *
     * This involves hashing the code verifier using SHA-256 and then encoding the hash using Base64 URL-safe encoding.
     *
     * @param codeVerifier The PKCE Code Verifier.
     * @return The corresponding Code Challenge.
     */
    private fun generateCodeChallenge(codeVerifier: String): String {
        val hashedBytes = sha256(codeVerifier)
        return base64UrlEncode(hashedBytes)
    }

    suspend fun getCodeVerifier(): String? {
        return appDatabase.getCodeVerifier()
    }

    private fun generateRandomState(length: Int = 32): String {
        val possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.map { possibleChars[random.nextInt(possibleChars.length)] }.joinToString("")
    }

    suspend fun getAuthState(): String? {
        return appDatabase.getAuthState()
    }

    // --- Authentication Flow ---

    /**
     * Initiates the Spotify PKCE Authorization flow.
     *
     * @param context The Activity or Application context.
     */
    suspend fun startAuthentication() {
        // 1. Generate Code Verifier
        val codeVerifier = generateCodeVerifier()

        // 2. Save Code Verifier for later token exchange
        //saveCodeVerifier(context, codeVerifier)
        appDatabase.saveCodeVerifier(codeVerifier)

        // 3. Generate Code Challenge
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // 4. Generate and Save State
        val state = generateRandomState()
        //saveState(context, state)
        appDatabase.saveAuthState(state)

        // 5. Build Authorization URL
        val authUri = AUTH_ENDPOINT.toUri().buildUpon().apply {
            appendQueryParameter("response_type", "code")
            appendQueryParameter("client_id", CLIENT_ID)
            appendQueryParameter("scope", SCOPES.joinToString(" "))
            appendQueryParameter("code_challenge_method", "S256")
            appendQueryParameter("code_challenge", codeChallenge)
            appendQueryParameter("redirect_uri", REDIRECT_URI)
            appendQueryParameter("state", state)
        }.build()

        // 6. Open the URL in a browser or Chrome Custom Tab
        val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Handle errors when a browser cannot be opened (e.g., no suitable browser installed)
            e.printStackTrace()
            // Consider displaying an error message to the user
            toast(context, "Error opening browser", Toast.LENGTH_SHORT)
        }
    }
}