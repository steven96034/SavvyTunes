package com.example.geminispotifyapp.auth

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.geminispotifyapp.BuildConfig
import com.example.geminispotifyapp.utils.toast


object AuthManager {

    // --- Constants ---

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
    private const val CODE_VERIFIER_PREF_KEY = "spotify_code_verifier"
    private const val PREF_NAME = "spotify_auth_prefs"
    private const val STATE_KEY = "spotify_auth_state"

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

    // --- Code Verifier Storage ---

    /**
     * Gets the SharedPreferences instance for this application.
     *
     * @param context The application context.
     * @return The SharedPreferences instance.
     */
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Saves the generated PKCE Code Verifier to SharedPreferences.
     *
     * @param context The application context.
     * @param codeVerifier The Code Verifier to save.
     */
    private fun saveCodeVerifier(context: Context, codeVerifier: String) {
        getSharedPreferences(context).edit {
            putString(CODE_VERIFIER_PREF_KEY, codeVerifier)
        }
    }

    /**
     * Retrieves the saved PKCE Code Verifier from SharedPreferences.
     *
     * This method is typically called after receiving the Authorization Code from Spotify,
     * when exchanging it for an Access Token.
     *
     * @param context The application context.
     * @return The saved Code Verifier, or `null` if not found.
     */
    fun getSavedCodeVerifier(context: Context): String? {
        return getSharedPreferences(context).getString(CODE_VERIFIER_PREF_KEY, null)
    }

    private fun generateRandomState(length: Int = 32): String {
        val possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.map { possibleChars[random.nextInt(possibleChars.length)] }.joinToString("")
    }

    fun getSavedState(context: Context): String? {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(STATE_KEY, null)
    }

    private fun saveState(context: Context, state: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().apply {
            putString(STATE_KEY, state)
            apply()
        }
    }
    // --- Authentication Flow ---

    /**
     * Initiates the Spotify PKCE Authorization flow.
     *
     * @param context The Activity or Application context.
     */
    fun startAuthentication(context: Context) {
        // 1. Generate Code Verifier
        val codeVerifier = generateCodeVerifier()

        // 2. Save Code Verifier for later token exchange
        saveCodeVerifier(context, codeVerifier)

        // 3. Generate Code Challenge
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // 4. Generate and Save State
        val state = generateRandomState()
        saveState(context, state)

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
        val intent = Intent(Intent.ACTION_VIEW, authUri)

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