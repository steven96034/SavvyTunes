package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService

class SpotifyRepository private constructor(private val context: Context, private val spotifyUserApiService: SpotifyUserApiService, private val spotifyApiService: SpotifyApiService) {

    companion object {
        const val PREF_NAME = "spotify_token_prefs"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val TOKEN_TYPE_KEY = "token_type"
        const val EXPIRES_AT_KEY = "expires_at"

        // Provide a public factory function to create an instance of SpotifyRepository in order not to be constructed by others outside.
        fun create(context: Context, spotifyUserApiService: SpotifyUserApiService, spotifyApiService: SpotifyApiService): SpotifyRepository {
            return SpotifyRepository(context.applicationContext, spotifyUserApiService, spotifyApiService)
        }
    }

    fun getAccessToken(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun updateAccessToken(newToken: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString(ACCESS_TOKEN_KEY, newToken)
            apply()
        }
    }

    // Always return "Bearer"
    private fun getTokenType(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_TYPE_KEY, "Bearer")
    }

    // Check if the token has expired
    private fun isTokenExpired(): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val expiresAt = prefs.getLong(EXPIRES_AT_KEY, 0)
        return System.currentTimeMillis() > expiresAt
    }

    // Get the authorization header
    private fun getAuthorizationHeader(): String {
        val tokenType = getTokenType() ?: "Bearer"
        val accessToken = getAccessToken() ?: ""
        return "$tokenType $accessToken"
    }

    suspend fun getUserTopArtists(
        timeRange: String = "medium_term",
        limit: Int = 20,
        offset: Int = 0
    ): TopArtistsResponse {
        if (isTokenExpired()) {
            refreshToken()
        }

        return spotifyUserApiService.getTopArtists(
            authorization = getAuthorizationHeader(),
            timeRange = timeRange,
            limit = limit,
            offset = offset
        )
    }

    suspend fun getUserTopTracks(
        timeRange: String = "medium_term",
        limit: Int = 20,
        offset: Int = 0
    ): TopTracksResponse {
        if (isTokenExpired()) {
            refreshToken()
        }

        return spotifyUserApiService.getTopTracks(
            authorization = getAuthorizationHeader(),
            timeRange = timeRange,
            limit = limit,
            offset = offset
        )
    }

    suspend fun getRecentlyPlayedTracks(
        limit: Int = 20,
        before: Long? = null,
        after: Long? = null
    ): RecentlyPlayedResponse {
        if (isTokenExpired()) {
            refreshToken()
        }

        return spotifyUserApiService.getRecentlyPlayed(
            authorization = getAuthorizationHeader(),
            limit = limit,
            before = before,
            after = after
        )
    }

    suspend fun searchData(
        query: String,
        type: String = "track",
        limit: Int = 10,
        offset: Int = 0,
        market: String? = null,
        includeExternal: String? = null
    ): SearchResponse {
        if (isTokenExpired()) {
            refreshToken()
        }
        return spotifyUserApiService.searchTracks(
            authorization = getAuthorizationHeader(),
            query = query,
            type = type,
            limit = limit,
            offset = offset,
            market = market,
            includeExternal = includeExternal
        )
    }

    private suspend fun refreshToken() {
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            Log.d("SpotifyData", "pref.accessToken before: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")

            // static refresh token method
            val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, "") ?: ""
            val clientId = BuildConfig.SPOTIFY_WEB_API_KEY
            if (refreshToken.isEmpty()) {
                Log.d("SpotifyData", "Empty Refresh Token")
                return
            }
            val requestBody = HashMap<String, String>().apply {
                put("grant_type", "refresh_token")
                put("refresh_token", refreshToken)
                put("client_id", clientId)
            }
            val response = spotifyApiService.refreshAccessToken(requestBody)

            // Save the new access token and expiration time
            with(prefs.edit()) {
                if (response != null) {
                    putString(ACCESS_TOKEN_KEY, response.accessToken)
                    Log.d("SpotifyData", "pref.accessToken after: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")

                    putLong(EXPIRES_AT_KEY, System.currentTimeMillis() + (response.expiresIn * 1000))

                    // refresh_token would changed when returned (not null)
                    if (response.refreshToken != null) {
                        putString(REFRESH_TOKEN_KEY, response.refreshToken)
                    }
                }
                apply()
            }
            Log.d("SpotifyData", "Successfully refreshed Access Token")
        } catch (e: Exception) {
            Log.e("SpotifyData refresh token", "Failed to refresh Access Token", e)
            throw e
        }
    }



    // static get access token method, call from AuthCallbackActivity
    suspend fun getAccessToken(
        grantType: String,
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String
    ): SpotifyTokenResponse {
        return spotifyApiService.getAccessToken(
            grantType = grantType,
            code = code,
            redirectUri = redirectUri,
            clientId = clientId,
            codeVerifier = codeVerifier
        )
    }
}