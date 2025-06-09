package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepository @Inject constructor(private val appDatabase: AppDatabase, private val spotifyUserApiService: SpotifyUserApiService, private val spotifyApiService: SpotifyApiService) {

//    companion object {
//        const val PREF_NAME = "spotify_token_prefs"
//        const val ACCESS_TOKEN_KEY = "access_token"
//        const val REFRESH_TOKEN_KEY = "refresh_token"
//        const val TOKEN_TYPE_KEY = "token_type"
//        const val EXPIRES_AT_KEY = "expires_at"
//
//        // Provide a public factory function to create an instance of SpotifyRepository in order not to be constructed by others outside.
//        fun create(appDatabase: AppDatabase, spotifyUserApiService: SpotifyUserApiService, spotifyApiService: SpotifyApiService): SpotifyRepository {
//            return SpotifyRepository(appDatabase, spotifyUserApiService, spotifyApiService)
//        }
//    }

    fun getAccessTokenFlow(): Flow<String?> {
        return appDatabase.getAccessTokenFlow()
    }

    suspend fun updateAccessToken(newToken: String) {
        appDatabase.saveAccessToken(newToken)
    }

    suspend fun updateTokenResponse(tokenResponse: SpotifyTokenResponse) {
        appDatabase.saveTokenResponse(tokenResponse)
    }
//
//    // Always return "Bearer"
//    private fun getTokenType(): String? {
//        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        return prefs.getString(TOKEN_TYPE_KEY, "Bearer")
//    }
//
//    // Check if the token has expired
//    private fun isTokenExpired(): Boolean {
//        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        val expiresAt = prefs.getLong(EXPIRES_AT_KEY, 0)
//        return System.currentTimeMillis() > expiresAt
//    }
//
//    // Get the authorization header
//    private fun getAuthorizationHeader(): String {
//        val tokenType = getTokenType() ?: "Bearer"
//        val accessToken = getAccessToken() ?: ""
//        return "$tokenType $accessToken"
//    }

    private suspend fun checkTokenAndGetHeader(): String {
        if (appDatabase.isTokenExpired()) {
            refreshToken()
        }
        val authHeader = appDatabase.getAuthorizationHeader()
        if (authHeader == null) {
            Log.e("SpotifyData", "Authorization header is null")
            throw IllegalStateException("Authorization header is null")
        }
        return authHeader
    }

    suspend fun getUserTopArtists(
        timeRange: String = "medium_term",
        limit: Int = 20,
        offset: Int = 0
    ): TopArtistsResponse {
        val authHeader = checkTokenAndGetHeader()

        return spotifyUserApiService.getTopArtists(
            authorization = authHeader,
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
        val authHeader = checkTokenAndGetHeader()

        return spotifyUserApiService.getTopTracks(
            authorization = authHeader,
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
        val authHeader = checkTokenAndGetHeader()

        return spotifyUserApiService.getRecentlyPlayed(
            authorization = authHeader,
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
        val authHeader = checkTokenAndGetHeader()
        return spotifyUserApiService.searchTracks(
            authorization = authHeader,
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
            //val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            //Log.d("SpotifyData", "pref.accessToken before: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")

            val refreshToken = appDatabase.getRefreshToken()
            val clientId = BuildConfig.SPOTIFY_WEB_API_KEY
            if (refreshToken.isNullOrEmpty()) {
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
            appDatabase.saveAccessToken(response.accessToken)
            appDatabase.saveExpiresAt(System.currentTimeMillis() + (response.expiresIn * 1000))

            if (response.refreshToken != null) {
                appDatabase.saveRefreshToken(response.refreshToken)
            }

//            with(prefs.edit()) {
//                if (response != null) {
//                    putString(ACCESS_TOKEN_KEY, response.accessToken)
//                    Log.d("AppDatabase", "pref.accessToken after: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")
//
//                    putLong(EXPIRES_AT_KEY, System.currentTimeMillis() + (response.expiresIn * 1000))
//
//                    // refresh_token would changed when returned (not null)
//                    if (response.refreshToken != null) {
//                        putString(REFRESH_TOKEN_KEY, response.refreshToken)
//                    }
//                }
//                apply()
//            }
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