package com.example.geminispotifyapp.data.remote

import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Spotify API service for fetching user data
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

    @GET("v1/search")
    suspend fun searchTracks(
        @Header("Authorization") authorization: String,
        @Query("q") query: String,
        @Query("type") type: String = "track",
        @Query("market") market: String? = "TW",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("include_external") includeExternal: String? = null
    ): SearchResponse
}