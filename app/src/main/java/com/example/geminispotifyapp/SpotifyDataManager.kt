package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

class SpotifyDataManager(private val context: Context) {

    companion object {
        private const val PREF_NAME = "spotify_token_prefs"
        private const val ACCESS_TOKEN_KEY = "access_token"
        private const val REFRESH_TOKEN_KEY = "refresh_token"
        private const val TOKEN_TYPE_KEY = "token_type"
        private const val EXPIRES_AT_KEY = "expires_at"
    }

    private val _accessTokenFlow = MutableStateFlow(getAccessToken()) // 使用 getAccessToken() 的初始值
    val accessTokenAsFlow: StateFlow<String?> = _accessTokenFlow

    // 從 SharedPreferences 取得 Access Token
    fun getAccessToken(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ACCESS_TOKEN_KEY, null)
    }

    fun updateAccessToken(accessToken: String?) {
        _accessTokenFlow.value = accessToken
        Log.d("Auth", "updateAccessToken: $accessToken")
    }

    // 從 SharedPreferences 取得 Token Type
    fun getTokenType(): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(TOKEN_TYPE_KEY, "Bearer")
    }

    // 檢查 Token 是否過期
    fun isTokenExpired(): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val expiresAt = prefs.getLong(EXPIRES_AT_KEY, 0)
        return System.currentTimeMillis() > expiresAt
    }

    // 取得授權 Header
    fun getAuthorizationHeader(): String {
        val tokenType = getTokenType() ?: "Bearer"
        val accessToken = getAccessToken() ?: ""
        return "$tokenType $accessToken"
    }

    // 取得用戶熱門藝術家
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

    // 取得用戶熱門歌曲
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

    // 取得用戶最近播放的歌曲
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

    // 刷新 Token
    private suspend fun refreshToken() {
        // 在正式版本中，您需要實現這個方法來使用 refresh_token 獲取新的 access_token
        // 下面是一個示例實現，您需要根據自己的 SpotifyApiService 來調整
        try {
            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            Log.d("SpotifyData", "pref.accessToken before: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")
            // val refreshToken = prefs.getString(REFRESH_TOKEN_KEY, null) ?: throw Exception("No refresh token available")

            val response = SpotifyApiService.refreshToken(context = context)

            // 保存新的 access_token 和過期時間
            with(prefs.edit()) {
                if (response != null) {
                    putString(ACCESS_TOKEN_KEY, response.accessToken)
                    Log.d("SpotifyData", "pref.accessToken after: ${prefs.getString(ACCESS_TOKEN_KEY, null)}")
                }
                if (response != null) {
                    putLong(EXPIRES_AT_KEY, System.currentTimeMillis() + (response.expiresIn * 1000))
                }
                // refresh_token 不一定每次都返回，只有在有變更時才返回
                if (response != null) {
                    if (response.refreshToken != null) {
                        putString(REFRESH_TOKEN_KEY, response.refreshToken)
                    }
                }
                apply()
            }

            Log.d("SpotifyData", "成功更新 Access Token")
        } catch (e: Exception) {
            Log.e("SpotifyData refresh token", "刷新 Token 失敗", e)
            throw e
        }
    }

    suspend fun refreshTokenDataIfNeeded() {
        try {
            SpotifyApiService.refreshTokenIfNeeded(context)
        } catch (e: Exception) {
            Log.e("SpotifyData", "刷新 Token 失敗", e)
            throw e
        }
    }

}