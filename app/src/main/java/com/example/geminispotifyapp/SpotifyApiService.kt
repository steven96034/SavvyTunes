package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import com.adamratzman.spotify.utils.Language
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface SpotifyApiService {
    @FormUrlEncoded
    @POST("api/token")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): SpotifyTokenResponse

    @FormUrlEncoded
    @POST("api/token")
    suspend fun refreshAccessToken(@FieldMap params: Map<String, String>): SpotifyTokenResponse

    companion object {
        private const val BASE_URL = "https://accounts.spotify.com/"

        // 創建 Retrofit 實例
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
                .build()
        }

        // 創建 OkHttpClient 實例
        private fun createOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()
        }

        // 提供 API 服務
        val service: SpotifyApiService by lazy {
            retrofit.create(SpotifyApiService::class.java)
        }

        // 靜態方法，用於從 AuthCallbackActivity 呼叫
        suspend fun getAccessToken(
            grantType: String,
            code: String,
            redirectUri: String,
            clientId: String,
            codeVerifier: String
        ): SpotifyTokenResponse {
            return service.getAccessToken(
                grantType = grantType,
                code = code,
                redirectUri = redirectUri,
                clientId = clientId,
                codeVerifier = codeVerifier
            )
        }

        // 靜態刷新 token 方法
        suspend fun refreshToken(context: Context): SpotifyTokenResponse? {
            try {
                // 從 SharedPreferences 獲取之前保存的 refresh token
                val sharedPreferences = context.getSharedPreferences("spotify_token_prefs", Context.MODE_PRIVATE)
                val refreshToken = sharedPreferences.getString("refresh_token", "") ?: ""
                val clientId = BuildConfig.SPOTIFY_WEB_API_KEY

                if (refreshToken.isEmpty()) {
                    return null
                }

                // 準備請求參數
                val requestBody = HashMap<String, String>().apply {
                    put("grant_type", "refresh_token")
                    put("refresh_token", refreshToken)
                    put("client_id", clientId)
                }

                // 發送請求
                val response = service.refreshAccessToken(requestBody)

                // 保存新的 access token 和 refresh token (如果有提供)
                sharedPreferences.edit().apply {
                    putString("access_token", response.accessToken)
                    response.refreshToken?.let {
                        putString("refresh_token", Language.it.toString())
                    }
                    putLong("token_expiry", System.currentTimeMillis() + (response.expiresIn * 1000))
                    apply()
                }

                return response
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        suspend fun refreshTokenIfNeeded(context: Context) {
            // 檢查 token 是否過期 (可以存儲 token 過期時間)
            val currentTime = System.currentTimeMillis() / 1000 // Convert to seconds
            val sharedPreferences = context.getSharedPreferences("spotify_token_prefs", Context.MODE_PRIVATE)
            val expiryTime = sharedPreferences.getLong("expires_at", 0)

            Log.d("SpotifyAuth", "Current time: $currentTime, Expiry time: $expiryTime")
            if (expiryTime <= currentTime) {
                // 使用 refresh token 獲取新的 access token
                Log.d("SpotifyAuth", "Access token expired. Refreshing...")
                val refreshToken = sharedPreferences.getString("refresh_token", null)
                if (refreshToken != null) {
                    try {
                        val newTokens = refreshToken(context)?.accessToken
                        // 儲存新的 tokens
                        with(sharedPreferences.edit()) {
                            putString("access_token", newTokens)
                            apply()
                        }
                        SpotifyDataManager(context).updateAccessToken(newTokens)
                        Log.d("SpotifyAuth", "(Refresh) Token 已重新儲存至 SharedPreferences")
                        Log.d("SpotifyAuth", "New tokens: ${sharedPreferences.getString("access_token", "not found")}, ${sharedPreferences.getLong("expires_at", 0)}")
                        //saveTokens(newTokens)
                    } catch (e: Exception) {
                        // Token 刷新失敗，需要重新登入
                        with(sharedPreferences.edit()) {
                            putString("access_token", null)
                            putString("refresh_token", null)
                            putString("token_type", null)
                            putLong("expires_at", 0)
                            putString("scope", null)
                            apply()
                        }
                        SpotifyDataManager(context).updateAccessToken(null)
                        //clearTokens()
                        throw Exception("認證已過期，請重新登入")
                    }
                } else {
                    // 沒有 refresh token，需要重新登入
                    with(sharedPreferences.edit()) {
                        putString("access_token", null)
                        putString("refresh_token", null)
                        putString("token_type", null)
                        putLong("expires_at", 0)
                        putString("scope", null)
                        apply()
                    }
                    SpotifyDataManager(context).updateAccessToken(null)
                    //clearTokens()
                    throw Exception("認證已過期，請重新登入")
                }
            } else {
                Log.d("SpotifyAuth", "Access token still valid.")
            }
        }
    }
}