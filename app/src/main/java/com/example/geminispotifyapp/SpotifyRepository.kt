package com.example.geminispotifyapp

import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.SimplifiedTracksResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.data.TracksResponse
import com.example.geminispotifyapp.data.UserProfileResponse
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.features.userdatadetail.FetchResultWithEtag

interface SpotifyRepository {
    suspend fun getAccessToken(): String

    fun getCurrentAccessToken(): String?

    suspend fun updateTokenResponse(tokenResponse: SpotifyTokenResponse)

    fun isTokenExpired(expiryTime: Long?): Boolean

    suspend fun performLogOutAndCleanUp()

    suspend fun getRecentlyPlayedTracks(
        limit: Int = 20,
        before: Long? = null,
        after: Long? = null
    ): FetchResult<RecentlyPlayedResponse>

    suspend fun searchData(
        query: String,
        type: String = "track",
        limit: Int = 10,
        offset: Int = 0,
        market: String? = null,
        includeExternal: String? = null
    ): FetchResult<SearchResponse>

    suspend fun getUserTopArtists(
        timeRange: String = "medium_term",
        limit: Int = 20,
        offset: Int = 0,
        ifNoneMatch: String? = null
    ): FetchResultWithEtag<TopArtistsResponse>

    suspend fun getUserTopTracks(
        timeRange: String = "medium_term",
        limit: Int = 20,
        offset: Int = 0,
        ifNoneMatch: String? = null
    ): FetchResultWithEtag<TopTracksResponse>

    suspend fun getAccessTokenThruAuth(
        grantType: String = "authorization_code",
        code: String,
        redirectUri: String,
        clientId: String,
        codeVerifier: String
    ): SpotifyTokenResponse

    suspend fun getUserProfile(): FetchResult<UserProfileResponse>

    suspend fun getTopTracksOfArtist(artistId: String): FetchResult<TracksResponse>

    suspend fun getAlbumTracks(albumId: String): FetchResult<SimplifiedTracksResponse>

    suspend fun getTrack(trackId: String, market: String? = null): FetchResult<SpotifyTrack>
}