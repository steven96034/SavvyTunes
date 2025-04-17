package com.example.geminispotifyapp

import android.content.Context
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
    }
}