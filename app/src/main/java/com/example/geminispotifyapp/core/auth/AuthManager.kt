package com.example.geminispotifyapp.core.auth

import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Log
import androidx.core.net.toUri
import com.example.geminispotifyapp.BuildConfig
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val appDatabase: AppDatabase,
    private val spotifyRepository: SpotifyRepository
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
//            "playlist-read-private", // Read access to user's private playlists.
//            "playlist-modify-private", // Write/delete access to the list of artists and other users that the user follows.
//            "user-follow-read", // Read access to the list of artists and other users that the user follows.
//            "user-library-modify", // Write/delete access to a user's "Your Music" library.
//            "user-library-read", // Read access to a user's library.
            "user-top-read", // Read access to a user's top artists and tracks.
            "user-read-recently-played", // Read access to a user’s recently played tracks.
            "user-read-private", // Read access to user’s subscription details (type of user account).
            "user-read-email" // Read access to user’s email address.
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

    private fun generateRandomState(length: Int = 32): String {
        val possibleChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return bytes.map { possibleChars[random.nextInt(possibleChars.length)] }.joinToString("")
    }

    // --- Authentication Flow ---

    suspend fun generateAuthorizationUrlAndSaveVerifier(): String {
        // 1. Generate Code Verifier
        val codeVerifier = generateCodeVerifier()

        // 2. Save Code Verifier for later token exchange
        appDatabase.saveCodeVerifier(codeVerifier)

        // 3. Generate Code Challenge
        val codeChallenge = generateCodeChallenge(codeVerifier)

        // 4. Generate and Save State
        val state = generateRandomState()
        appDatabase.saveAuthState(state)

        // 5. Build Authorization URL
        return AUTH_ENDPOINT.toUri().buildUpon().apply {
            appendQueryParameter("response_type", "code")
            appendQueryParameter("client_id", CLIENT_ID)
            appendQueryParameter("scope", SCOPES.joinToString(" "))
            appendQueryParameter("code_challenge_method", "S256")
            appendQueryParameter("code_challenge", codeChallenge)
            appendQueryParameter("redirect_uri", REDIRECT_URI)
            appendQueryParameter("state", state)
        }.build().toString()
    }

    suspend fun exchangeCodeForToken(code: String, receivedState: String): Boolean {
        val savedCodeVerifier = appDatabase.getCodeVerifier()
        val savedAuthState = appDatabase.getAuthState()

        // Clean up verifier and state regardless of outcome
        appDatabase.deleteCodeVerifier()
        appDatabase.deleteAuthState()

        if (savedCodeVerifier == null) {
            Log.e("AuthManager", "Saved Code Verifier not found!")
            return false
        }
        if (savedAuthState == null || receivedState != savedAuthState) {
            Log.e("AuthManager", "Received state does not match saved state, potential CSRF attack!")
            return false
        }

        return try {
            val response = spotifyRepository.getAccessTokenThruAuth(
                grantType = "authorization_code",
                code = code,
                redirectUri = REDIRECT_URI,
                clientId = CLIENT_ID,
                codeVerifier = savedCodeVerifier
            )
            spotifyRepository.updateTokenResponse(response)
            true
        } catch (e: Exception) {
            Log.e("AuthManager", "Error exchanging Token", e)
            false
        }
    }
}