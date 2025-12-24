package com.example.geminispotifyapp.data.repository

import android.util.Log
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.BuildConfig
import com.example.geminispotifyapp.data.remote.model.SpotifyTokenResponse
import com.example.geminispotifyapp.core.di.ApplicationScope
import com.example.geminispotifyapp.data.remote.model.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.remote.model.SearchResponse
import com.example.geminispotifyapp.data.remote.model.SimplifiedTracksResponse
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.remote.model.TopArtistsResponse
import com.example.geminispotifyapp.data.remote.model.TopTracksResponse
import com.example.geminispotifyapp.data.remote.model.TracksResponse
import com.example.geminispotifyapp.data.remote.model.UserProfileResponse
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.api.SpotifyApiService
import com.example.geminispotifyapp.data.remote.api.SpotifyUserApiService
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.utils.ApiExecutionHelper
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.core.utils.FetchResultWithEtag
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyRepositoryImpl @Inject constructor(
    private val appDatabase: AppDatabase,
    private val spotifyUserApiService: SpotifyUserApiService,
    private val spotifyApiService: SpotifyApiService,
    private val apiExecutionHelper: ApiExecutionHelper, // Inject ApiExecutionHelper
    private val firestore: FirebaseFirestore,      // Remote Firestore
    private val auth: FirebaseAuth,                 // Check current user
    @ApplicationScope private val applicationScope: CoroutineScope
) : SpotifyRepository {
    private val tag = "SpotifyRepository"
    private val tokenRefreshMutex = Mutex() // Ensure that only one refresh is in progress
    private var isRefreshing = false // Prevent multiple concurrent refreshes inside of tokenRefreshMutex

    // Cache the current access token and expiry time in memory to avoid frequent database reads and more effective token refresh
    private val _currentAccessTokenFlow = MutableStateFlow<String?>(null)
    override val currentAccessTokenFlow: StateFlow<String?> = _currentAccessTokenFlow
    private val _currentTokenExpiryTimeFlow = MutableStateFlow<Long?>(null)

    override val searchSimilarNumFlow: Flow<Int> = appDatabase.searchSimilarNumFlow
    override val userDataNumFlow: Flow<Int> = appDatabase.getUserDataNumFlow

    override val checkMarketIfPlayableFlow: Flow<String?> = appDatabase.checkMarketIfPlayableFlow

    override val numOfShowCaseSearchFlow: Flow<Int> = appDatabase.numOfShowCaseSearchFlow
    override val languageOfShowCaseSearchFlow: Flow<String> = appDatabase.languageOfShowCaseSearchFlow
    override val genreOfShowCaseSearchFlow: Flow<String> = appDatabase.genreOfShowCaseSearchFlow
    override val yearOfShowCaseSearchFlow: Flow<String> = appDatabase.yearOfShowCaseSearchFlow
    override val isRandomYearOfShowCaseSelectionFlow: Flow<Boolean> = appDatabase.isRandomYearOfShowCaseSelectionFlow


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

            // Perform actual token refresh in a coroutine
            if (!isRefreshing) { // Avoid multiple concurrent refreshes inside of tokenRefreshMutex
                isRefreshing = true
                try {
                    val refreshToken = appDatabase.getRefreshToken()
                    if (refreshToken == null) {
                        // Use ApiError.Unauthorized to indicate that the user needs to re-authenticate.
                        Log.e(tag, "Refresh token not found. User needs to re-authenticate.")
                        throw ApiError.Unauthorized("Refresh token not found. Needs to re-authenticate.")
                    }

                    try {
                        val newTokens = performActualTokenRefresh(refreshToken)
                        return@withLock newTokens
                    } catch (e: Exception) {
                        if (e is ApiError.BadRequest && e.message?.contains("invalid_grant") == true) {
                            // Local refresh failed (Refresh Token expired/replaced)
                            // Get latest refresh token from Firestore (Server-side may have been updated)

                            Log.w(tag, "Local refresh token failed. Trying to fetch from Firestore...")
                            try {
                                val serverRefreshToken = fetchRefreshTokenFromFirestore()

                                if (serverRefreshToken != null) {
                                    // For later check if the refresh token is revoked.
                                    appDatabase.saveRefreshToken(serverRefreshToken)
                                    val newAccessToken =
                                        performActualTokenRefresh(serverRefreshToken)
                                    Log.i(tag, "Rescued! Token updated from Firestore.")
                                    return@withLock newAccessToken
                                }
                            } catch (rescueException: Exception) {
                                Log.e(tag, "Firestore rescue failed.", rescueException)
                            }
                            throw ApiError.Unauthorized("Failed to refresh token: Refresh token revoked. Need to re-authenticate.")
                        }
                        else if (e is HttpException && e.code() == 401) {
                            // Because the request that refresh access token has received HTTP 401 Unauthorized, that usually means refresh_token is invalid or revoked.
                            Log.e(tag, "HTTP 401 on refreshing token. Refresh token might be invalid.", e)

                            // Clear all session tokens to try to resign in.
                            applicationScope.launch {
                                _currentAccessTokenFlow.value = null
                                _currentTokenExpiryTimeFlow.value = null
                            }
                            // Use ApiError.Unauthorized to indicate that the user needs to re-authenticate.
                            throw ApiError.Unauthorized("Failed to refresh token: HTTP 401, refresh token likely invalid. Need to re-authenticate.")
                        }
                        // For other exceptions (network issues, server 5xx errors, etc.)
                        Log.e(tag, "Failed to refresh token with other exception.", e)
                        // Use ApiError.TooManyRequests to indicate that the user needs to wait before retrying.
                        throw ApiError.TooManyRequests("Failed to refresh token: ${e.message}", null)
                    }
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

    private suspend fun fetchRefreshTokenFromFirestore(): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            withContext(Dispatchers.IO) {
                // Tasks.await() is safe to be called here
                val snapshot = Tasks.await(
                    firestore.collection("users").document(uid)
                        .collection("private_data").document("spotify_secrets").get()
                )
                snapshot.getString("refreshToken")
            }
        } catch (e: Exception) {
            Log.e(tag, "Firestore rescue failed when fetching refresh token.", e)
            null
        }
    }

    override fun getCurrentAccessToken(): String? {
        return _currentAccessTokenFlow.value
    }

    override suspend fun updateTokenResponse(tokenResponse: SpotifyTokenResponse) {
        appDatabase.saveTokenResponse(tokenResponse)

        try {
            val currentUser = auth.currentUser
            if (currentUser != null && tokenResponse.refreshToken != null) {
                val secretData = hashMapOf(
                    "refreshToken" to tokenResponse.refreshToken,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users")
                    .document(currentUser.uid)
                    .collection("private_data")
                    .document("spotify_secrets")
                    .set(secretData, SetOptions.merge())
                    .await()
            } else {
                Log.d(tag, "No user is currently signed in or some problem occurred.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save refresh token through updateTokenResponse", e)
        }
    }

    override fun isTokenExpired(expiryTime: Long?): Boolean {
        if (expiryTime == null) return true
        return System.currentTimeMillis() > expiryTime
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
                        limit = userDataNumFlow.first(),
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
                        limit = userDataNumFlow.first(),
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
                limit = userDataNumFlow.first(),
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
    ): FetchResult<SearchResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.searchTracks(
                authorization = authHeader,
                query = query,
                type = type,
                limit = limit,
                offset = offset,
                market = market,
                includeExternal = includeExternal
            )
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to search data", e)
            return FetchResult.Error(e)
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
        _currentTokenExpiryTimeFlow.value =
            System.currentTimeMillis() + (response.expiresIn * 1000) - REFRESH_BEFORE_EXPIRY_MS
        val uid = auth.currentUser?.uid
        Log.d(tag, "Firebase User: ${auth.currentUser}, ID: $uid")
        try {
            if (uid != null && response.refreshToken != null) {
                val secretData = hashMapOf(
                    "refreshToken" to response.refreshToken,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users")
                    .document(uid)
                    .collection("private_data")
                    .document("spotify_secrets")
                    .set(secretData, SetOptions.merge())
                    .await()
                Log.d(tag, "Refresh token saved to Firestore.")
            } else {
                Log.d(tag, "No user is currently signed in or some problem occurred.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to save refresh token through performActualTokenRefresh", e)
        }

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

    override suspend fun getUserProfile(): FetchResult<UserProfileResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.getUserProfile(authHeader)
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get user profile", e)
            return FetchResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get user profile", e)
            throw e
        }
    }

    override suspend fun getTopTracksOfArtist(artistId: String): FetchResult<TracksResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.getTopTracksByArtist(authHeader, artistId)
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get top tracks of artist", e)
            return FetchResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get top tracks of artist", e)
            throw e
        }
    }

    override suspend fun getAlbumTracks(albumId: String): FetchResult<SimplifiedTracksResponse> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.getAlbumTracks(authHeader, albumId)
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get album tracks", e)
            return FetchResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get album tracks", e)
            throw e
        }
    }

    override suspend fun getTrack(trackId: String, market: String?): FetchResult<SpotifyTrack> {
        try {
            val authHeader = getAuthorizationHeader()
            val result = spotifyUserApiService.getTrack(
                authorization = authHeader,
                trackId = trackId,
                market = market
            )
            return FetchResult.Success(result)
        } catch (e: ApiError) {
            Log.e(tag, "Failed to get track details for ID: $trackId", e)
            return FetchResult.Error(e)
        } catch (e: Exception) {
            Log.e(tag, "Failed to get track details for ID: $trackId", e)
            throw e
        }
    }

    override suspend fun setSearchSimilarNum(searchNum: Int) {
        appDatabase.saveSearchSimilarNum(searchNum)
    }

    override suspend fun setUserDataNum(dataNum: Int) {
        appDatabase.saveGetUserDataNum(dataNum)
    }

    override suspend fun setCheckMarketIfPlayable(market: String?) {
        appDatabase.saveCheckMarketIfPlayableFlow(market)
    }

    override suspend fun setNumOfShowCaseSearch(num: Int) {
        appDatabase.saveNumOfShowCaseSearch(num)
    }

    override suspend fun setLanguageOfShowCaseSearch(language: String) {
        appDatabase.saveLanguageOfShowCaseSearch(language)
        val uid = auth.currentUser?.uid ?: return
        try {
            val settingsData = hashMapOf(
                "language" to language,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .collection("preferences")
                .document("music_settings")
                .set(settingsData, SetOptions.merge())
                .await()
            Log.d(tag, "Language settings saved to firestore.")

        } catch (e: Exception) {
            Log.d(tag, "Failed to save language settings to firestore through setLanguageOfShowCaseSearch", e)
        }
    }

    override suspend fun setGenreOfShowCaseSearch(genre: String) {
        appDatabase.saveGenreOfShowCaseSearch(genre)
        val uid = auth.currentUser?.uid ?: return
        try {
            val settingsData = hashMapOf(
                "genre" to genre,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .collection("preferences")
                .document("music_settings")
                .set(settingsData, SetOptions.merge())
                .await()
            Log.d(tag, "Genre settings saved to firestore.")

        } catch (e: Exception) {
            Log.d(tag, "Failed to save genre settings to firestore through setLanguageOfShowCaseSearch", e)
        }
    }

    override suspend fun setYearOfShowCaseSearch(year: String) {
        appDatabase.saveYearOfShowCaseSearch(year)
        val uid = auth.currentUser?.uid ?: return
        try {
            val settingsData = hashMapOf(
                "year" to year,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .collection("preferences")
                .document("music_settings")
                .set(settingsData, SetOptions.merge())
                .await()
            Log.d(tag, "Year settings saved to firestore.")

        } catch (e: Exception) {
            Log.d(tag, "Failed to save year settings to firestore through setLanguageOfShowCaseSearch", e)
        }
    }

    override suspend fun setIsRandomYearOfShowCaseSelection(isRandom: Boolean) {
        appDatabase.saveIsRandomYearOfShowCaseSelection(isRandom)
        val uid = auth.currentUser?.uid ?: return
        try {
            val settingsData = hashMapOf(
                "isRandom" to isRandom,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore.collection("users")
                .document(uid)
                .collection("preferences")
                .document("music_settings")
                .set(settingsData, SetOptions.merge())
                .await()
            Log.d(tag, "isRandom settings saved to firestore.")

        } catch (e: Exception) {
            Log.d(tag, "Failed to save isRandom settings to firestore through setLanguageOfShowCaseSearch", e)
        }
    }

    companion object {
        // Check expiry as 1 minute before and refresh token
        private const val REFRESH_BEFORE_EXPIRY_MS = 60 * 1000
    }
}