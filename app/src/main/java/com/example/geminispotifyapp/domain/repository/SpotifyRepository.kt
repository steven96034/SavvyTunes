package com.example.geminispotifyapp.domain.repository

import com.example.geminispotifyapp.data.remote.model.SpotifyTokenResponse
import com.example.geminispotifyapp.data.remote.model.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.remote.model.SearchResponse
import com.example.geminispotifyapp.data.remote.model.SimplifiedTracksResponse
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.remote.model.TopArtistsResponse
import com.example.geminispotifyapp.data.remote.model.TopTracksResponse
import com.example.geminispotifyapp.data.remote.model.TracksResponse
import com.example.geminispotifyapp.data.remote.model.UserProfileResponse
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.core.utils.FetchResultWithEtag
import kotlinx.coroutines.flow.Flow

interface SpotifyRepository {
    val currentAccessTokenFlow: Flow<String?>
    val searchSimilarNumFlow: Flow<Int>
    val userDataNumFlow: Flow<Int>
    val checkMarketIfPlayableFlow: Flow<String?>

    val numOfShowCaseSearchFlow: Flow<Int>
    val languageOfShowCaseSearchFlow: Flow<String?>
    val genreOfShowCaseSearchFlow: Flow<String?>
    val yearOfShowCaseSearchFlow: Flow<String?>


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

    suspend fun setSearchSimilarNum(searchNum: Int)

    suspend fun setUserDataNum(dataNum: Int)

    suspend fun setCheckMarketIfPlayable(market: String?)

    suspend fun setNumOfShowCaseSearch(num: Int)
    suspend fun setLanguageOfShowCaseSearch(language: String?)
    suspend fun setGenreOfShowCaseSearch(genre: String?)
    suspend fun setYearOfShowCaseSearch(year: String?)
}