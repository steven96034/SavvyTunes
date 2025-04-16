package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Spotify 用戶 API 服務介面
interface SpotifyUserApiService {
    @GET("v1/me/top/artists")
    suspend fun getTopArtists(
        @Header("Authorization") authorization: String,
        @Query("time_range") timeRange: String = "medium_term", // short_term, medium_term, long_term
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): TopArtistsResponse

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") authorization: String,
        @Query("time_range") timeRange: String = "medium_term", // short_term, medium_term, long_term
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): TopTracksResponse

    @GET("v1/me/player/recently-played")
    suspend fun getRecentlyPlayed(
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 20,
        @Query("before") before: Long? = null,
        @Query("after") after: Long? = null
    ): RecentlyPlayedResponse

    companion object {
        private const val BASE_URL = "https://api.spotify.com/"

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
        val service: SpotifyUserApiService by lazy {
            retrofit.create(SpotifyUserApiService::class.java)
        }
    }
}