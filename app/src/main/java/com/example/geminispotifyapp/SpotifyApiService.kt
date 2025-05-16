package com.example.geminispotifyapp

import android.content.Context
import com.example.geminispotifyapp.SpotifyRepository.Companion.PREF_NAME
import com.example.geminispotifyapp.SpotifyRepository.Companion.REFRESH_TOKEN_KEY
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

        // Create Retrofit instance
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient())
                .build()
        }

        // Create OkHttpClient instance
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

        // Provide API service
        private val service: SpotifyApiService by lazy {
            retrofit.create(SpotifyApiService::class.java)
        }

        // static get access token method, call from AuthCallbackActivity
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

        // static refresh token method
        suspend fun refreshToken(context: Context): SpotifyTokenResponse? {
            try {
                val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                val refreshToken = sharedPreferences.getString(REFRESH_TOKEN_KEY, "") ?: ""
                val clientId = BuildConfig.SPOTIFY_WEB_API_KEY

                if (refreshToken.isEmpty()) {
                    return null
                }

                // Prepare request parameters
                val requestBody = HashMap<String, String>().apply {
                    put("grant_type", "refresh_token")
                    put("refresh_token", refreshToken)
                    put("client_id", clientId)
                }

                // Send request and get response
                val response = service.refreshAccessToken(requestBody)

//                // 保存新的 access token 和 refresh token (如果有提供)
//                sharedPreferences.edit().apply {
//                    putString(ACCESS_TOKEN_KEY, response.accessToken)
//                    response.refreshToken?.let {
//                        putString(REFRESH_TOKEN_KEY, Language.it.toString())
//                    }
//                    putLong(EXPIRES_AT_KEY, System.currentTimeMillis() + (response.expiresIn * 1000))
//                    apply()
//                }

                return response
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }
}