package com.example.geminispotifyapp

import android.util.Log
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.SpotifyTrack // Required for the new getTrack method
import com.example.geminispotifyapp.data.SimplifiedTracksResponse
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.data.TracksResponse
import com.example.geminispotifyapp.data.UserProfileResponse
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService
import com.example.geminispotifyapp.di.ApplicationScope
import com.example.geminispotifyapp.domain.TokenRefreshFailedException
import com.example.geminispotifyapp.domain.UserReAuthenticationRequiredException
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper // Import ApiExecutionHelper
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.features.userdatadetail.FetchResultWithEtag // Import FetchResultWithEtag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val spotifyUserApiService: SpotifyUserApiService,
    private val spotifyApiService: SpotifyApiService,
    private val apiExecutionHelper: ApiExecutionHelper, // Inject ApiExecutionHelper
    @ApplicationScope private val applicationScope: CoroutineScope
) : SpotifyRepository {
    private val tag = "SpotifyRepository"
    private val tokenRefreshMutex = Mutex() // Ensure that only one refresh is in progress
    private var isRefreshing = false // Prevent multiple concurrent refreshes inside of tokenRefreshMutex

    // Cache the current access token and expiry time in memory to avoid frequent database reads and more effective token refresh
    private val _currentAccessTokenFlow = MutableStateFlow<String?>(null)
    val currentAccessTokenFlow: StateFlow<String?> = _currentAccessTokenFlow
    private val _currentTokenExpiryTimeFlow = MutableStateFlow<Long?>(null)

    init {
        // Launch coroutine in ApplicationScope to collect data from DataStore
        applicationScope.launch {
            appDatabase.getAccessTokenFlow().collect { token ->
                _currentAccessTokenFlow.value = token
            }
        }
        applicationScope.launch {
            appDatabase.getExpiresAtFlow().collect { expiry ->
                _currentTokenExpiryTimeFlow.value = expiry
            }
        }
    }

    override suspend fun getAccessToken(): String {
        // Fast path: If the token is valid and not expired, return it without making another request
        val currentAccessToken = _currentAccessTokenFlow.value
        val currentTokenExpiryTime = _currentTokenExpiryTimeFlow.value

        if (currentAccessToken != null && !isTokenExpired(currentTokenExpiryTime)) {
            return currentAccessToken
        }

        // If token is expired or doesn't exist, try to refresh it in sync block
        return tokenRefreshMutex.withLock {
            // Check token again if token has been refreshed while waiting for Mutex
            if (currentAccessToken != null && !isTokenExpired(currentTokenExpiryTime)) {
                return currentAccessToken
            }


            // TODO: Try to integrate the interceptor with centralized error handling.
            // Perform actual token refresh in a coroutine
            if (!isRefreshing) { // Avoid multiple concurrent refreshes inside of tokenRefreshMutex
                isRefreshing = true
                try {
                    val refreshToken = appDatabase.getRefreshToken()
                    if (refreshToken == null) {
                        // TODO: Catch and set UI to AuthenticationExpiredContent() in ViewModel
                        throw UserReAuthenticationRequiredException("Refresh token not found. User needs to re-authenticate.")
                    }

                    // Call refresh token API from Spotify
                    val newTokens = performActualTokenRefresh(refreshToken)

                    return newTokens
                }  catch (e: Exception) {
                    if (e is HttpException && e.code() == 401) {
                        // Because the request that refresh access token has received HTTP 401 Unauthorized, that usually means refresh_token is invalid or revoked.
                        Log.e(tag, "HTTP 401 on refreshing token. Refresh token might be invalid.", e)

                        // Clear all session tokens to try to resign in. -
                        applicationScope.launch {
                            appDatabase.logout()
                            _currentAccessTokenFlow.value = null
                            _currentTokenExpiryTimeFlow.value = null
                        }

                        // Throw a more specific exception, indicating that re-authentication is required.
                        // TODO: Catch and set UI to AuthenticationExpiredContent() in ViewModel
                        throw UserReAuthenticationRequiredException("Failed to refresh token: HTTP 401, refresh token likely invalid. User needs to re-authenticate.", e)
                    }
                    // For other exceptions (network issues, server 5xx errors, etc.)
                    // TODO: Catch and use snackbar in viewModel and let user retry later to refresh or automatically retry after a fixed duration (through another function)
                    Log.e(tag, "Failed to refresh token with other exception.", e)
                    throw TokenRefreshFailedException("Failed to refresh token: ${e.message}", e)
                } finally {
                    // Ensure isRefreshing is reset even if an exception occurs
                    isRefreshing = false
                }
            } else {
                // If there's already a refresh in progress, wait for it to complete and return the new token
                currentAccessToken
                    ?: throw IllegalStateException("Failed to get access token after refresh.")
            }
        }
    }

    override fun getCurrentAccessToken(): String? {
        return _currentAccessTokenFlow.value
    }

    override suspend fun updateTokenResponse(tokenResponse: SpotifyTokenResponse) {
        appDatabase.saveTokenResponse(tokenResponse)
    }

    override fun isTokenExpired(expiryTime: Long?): Boolean {
        if (expiryTime == null) return true
        return System.currentTimeMillis() > expiryTime

        //return appDatabase.isTokenExpired()
    }

    override suspend fun performLogOutAndCleanUp() {
        appDatabase.logout()
    }


    override suspend fun getUserTopArtists(
        timeRange: String,
        limit: Int,
        offset: Int,
        ifNoneMatch: String?
    ): FetchResultWithEtag<TopArtistsResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            return apiExecutionHelper.executeEtaggedOperation(
                operation = {
                    spotifyUserApiService.getTopArtists(
                        authorization = authHeader,
                        ifNoneMatch = ifNoneMatch,
                        timeRange = timeRange,
                        limit = limit,
                        offset = offset
                    )
                },
                transformSuccess = { it }
            )
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get top artists: ${e.message}", e)
            return FetchResultWithEtag.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get top artists: ${e.message}", e)
            return FetchResultWithEtag.Error(ApiError.UnknownError("An unknown error occurred: ${e.message}"))
        }
    }

    override suspend fun getUserTopTracks(
        timeRange: String,
        limit: Int,
        offset: Int,
        ifNoneMatch: String?
    ): FetchResultWithEtag<TopTracksResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            return apiExecutionHelper.executeEtaggedOperation(
                operation = {
                    spotifyUserApiService.getTopTracks(
                        authorization = authHeader,
                        ifNoneMatch = ifNoneMatch,
                        timeRange = timeRange,
                        limit = limit,
                        offset = offset
                    )
                },
                transformSuccess = { it }
            )
        }
        catch (e: ApiError) {
            Log.e(tag, "Failed to get top tracks", e)
            return FetchResultWithEtag.Error(e)
        }
        catch (e: Exception) {
            Log.e(tag, "Failed to get top tracks", e)
            return FetchResultWithEtag.Error(ApiError.UnknownError("An unknown error occurred"))
        }
    }

    override suspend fun getRecentlyPlayedTracks(
        limit: Int,
        before: Long?,
        after: Long?
    ): FetchResult<RecentlyPlayedResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.getRecentlyPlayed(
                authorization = authHeader,
                limit = limit,
                before = before,
                after = after
            )
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get recently played tracks", e)
            return FetchResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get recently played tracks", e)
            throw e
        }
    }

    override suspend fun searchData(
        query: String,
        type: String,
        limit: Int,
        offset: Int,
        market: String?,
        includeExternal: String?
    ): SearchResponse {
        try {
            val authHeader = getAuthorizationHeader()
            return spotifyUserApiService.searchTracks(
                authorization = authHeader,
                query = query,
                type = type,
                limit = limit,
                offset = offset,
                market = market,
                includeExternal = includeExternal
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to search data", e)
            throw e
        }
    }

    private suspend fun getAuthorizationHeader(): String {
        try {
            val accessToken = getAccessToken()
            return "Bearer $accessToken"
        } catch (e: Exception) {
            Log.e(tag, "Failed to get access token", e)
            throw e
        }
    }

    // Only call from getAccessToken() to refresh token
    private suspend fun performActualTokenRefresh(refreshToken: String): String {
        // Call refresh token API from Spotify
        val clientId = BuildConfig.SPOTIFY_WEB_API_KEY

        val response = spotifyApiService.refreshAccessToken(
            refreshToken = refreshToken,
            clientId = clientId
        )
        // Save new Access Token and Refresh Token
        appDatabase.saveAccessToken(response.accessToken)
        appDatabase.saveRefreshToken(response.refreshToken)
        appDatabase.saveExpiresAt(System.currentTimeMillis() + (response.expiresIn * 1000) - REFRESH_BEFORE_EXPIRY_MS)
        // Update the Access Token and Expiry Time in memory
        _currentAccessTokenFlow.value = response.accessToken
        _currentTokenExpiryTimeFlow.value = System.currentTimeMillis() + (response.expiresIn * 1000) - REFRESH_BEFORE_EXPIRY_MS
        return response.accessToken
    }

    // Static get access token method, call from AuthCallbackActivity
    override suspend fun getAccessTokenThruAuth(
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

    override suspend fun getUserProfile(): UserProfileResponse {
        try {
            val authHeader = getAuthorizationHeader()
            return spotifyUserApiService.getUserProfile(authHeader)
        } catch (e: Exception) {
            Log.d(tag, "Failed to get user profile $e")
            throw e
        }
    }

    override suspend fun getTopTracksOfArtist(artistId: String): TracksResponse {
        try {
            val authHeader = getAuthorizationHeader()
            return spotifyUserApiService.getTopTracksByArtist(authHeader, artistId)
        }
        catch (e: Exception) {
            Log.d(tag, "Failed to get top tracks of artist $e")
            throw e
        }
    }

    override suspend fun getAlbumTracks(albumId: String): SimplifiedTracksResponse {
        try {
            val authHeader = getAuthorizationHeader()
            return spotifyUserApiService.getAlbumTracks(authHeader, albumId)
        }
        catch (e: Exception) {
            Log.d(tag, "Failed to get album tracks $e")
            throw e
        }
    }

    override suspend fun getTrack(trackId: String, market: String?): SpotifyTrack {
        try {
            val authHeader = getAuthorizationHeader()
            return spotifyUserApiService.getTrack(
                authorization = authHeader,
                trackId = trackId,
                market = market
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to get track details for ID: $trackId", e)
            throw e
        }
    }


    companion object {
        // Check expiry as 1 minute before and refresh token
        private const val REFRESH_BEFORE_EXPIRY_MS = 60 * 1000
    }
}