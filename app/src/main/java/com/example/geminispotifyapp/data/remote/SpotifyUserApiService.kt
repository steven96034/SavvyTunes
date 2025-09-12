package com.example.geminispotifyapp.data.remote

import com.example.geminispotifyapp.data.SimplifiedTracksResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.SpotifyTrack // <-- Add this import
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.data.TracksResponse
import com.example.geminispotifyapp.data.UserProfileResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

// Spotify API service for fetching user data
interface SpotifyUserApiService {
    @GET("v1/me/top/artists")
    suspend fun getTopArtists(
        @Header("Authorization") authorization: String,
        @Header("If-None-Match") ifNoneMatch: String? = null, // Add If-None-Match header for ETag
        @Query("time_range") timeRange: String = "medium_term", // short_term, medium_term, long_term
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<TopArtistsResponse> // Change return type to Response to get ETag

    @GET("v1/me/top/tracks")
    suspend fun getTopTracks(
        @Header("Authorization") authorization: String,
        @Header("If-None-Match") ifNoneMatch: String? = null, // Add If-None-Match header for ETag
        @Query("time_range") timeRange: String = "medium_term", // short_term, medium_term, long_term
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<TopTracksResponse> // Change return type to Response to get ETag

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

    @GET("v1/me")
    suspend fun getUserProfile(
        @Header("Authorization") authorization: String
    ): UserProfileResponse

    // Use simplified-tracks structure from Spotify API response to easily display in UI.
    @GET("v1/artists/{id}/top-tracks")
    suspend fun getTopTracksByArtist(
        @Header("Authorization") authorization: String,
        @Path("id") artistId: String,
        @Query("market") market: String? = null,
    ): TracksResponse

    @GET("v1/albums/{id}/tracks")
    suspend fun getAlbumTracks(
        @Header("Authorization") authorization: String,
        @Path("id") albumId: String,
        @Query("market") market: String? = "TW",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): SimplifiedTracksResponse

    @GET("v1/tracks/{id}")
    suspend fun getTrack(
        @Header("Authorization") authorization: String,
        @Path("id") trackId: String,
        @Query("market") market: String? = null
    ): SpotifyTrack

}