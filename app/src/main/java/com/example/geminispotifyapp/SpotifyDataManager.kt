package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SpotifyDataManager(private val context: Context) {

    companion object {
        const val PREF_NAME = "spotify_token_prefs"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
        const val TOKEN_TYPE_KEY = "token_type"
        const val EXPIRES_AT_KEY = "expires_at"
    }

    private val _accessTokenFlow = MutableStateFlow(getAccessToken()) // 使用 getAccessToken() 的初始值
    val accessTokenAsFlow: StateFlow<String?> = _accessTokenFlow

    private fun getAccessToken(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun updateAccessToken(accessToken: String?) {
        _accessTokenFlow.value = accessToken
        Log.d("Auth", "updateAccessToken: $accessToken")
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

        return SpotifyUserApiService.service.getTopArtists(
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

        return SpotifyUserApiService.service.getTopTracks(
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

        return SpotifyUserApiService.service.getRecentlyPlayed(
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
        return SpotifyUserApiService.service.searchTracks(
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

            val response = SpotifyApiService.refreshToken(context = context)

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
}