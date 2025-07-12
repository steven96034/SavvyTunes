package com.example.geminispotifyapp.init.userdata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.DownLoadState
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpotifyDataViewModel @Inject constructor(private val spotifyRepositoryImpl: SpotifyRepositoryImpl, private val authManager: AuthManager) : ViewModel() {

    private val _downLoadState: MutableStateFlow<DownLoadState> = MutableStateFlow(DownLoadState.Initial)
    val downLoadState: StateFlow<DownLoadState> = _downLoadState

    fun startAuthentication() {
        viewModelScope.launch {
            authManager.startAuthentication()
        }
    }

    // Define a function to fetch data
    fun fetchData() {
//        if (_downLoadState.value is DownLoadState.Loading || _downLoadState.value is DownLoadState.Success) {
//            return
//        }
//
//        _downLoadState.value = DownLoadState.Loading
//
//        viewModelScope.launch {
//            try {
//                // Use supervisorScope to ensure that a single task fails doesn't cancel other tasks
//                supervisorScope {
//                    Log.d("SpotifyDataScreen", "Fetching data...")
//                    //if (spotifyRepository.isTokenExpired()) spotifyRepository.refreshToken()
//
//                    // Use async to fetch data in parallel
//                    val topArtistsDeferredShort = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopArtists(
//                            timeRange = "short_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topArtistDeferredMedium = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopArtists(
//                            timeRange = "medium_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topArtistsDeferredLong = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopArtists(
//                            timeRange = "long_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//
//                    val topTracksDeferredShort = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopTracks(
//                            timeRange = "short_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topTracksDeferredMedium = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopTracks(
//                            timeRange = "medium_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topTracksDeferredLong = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getUserTopTracks(
//                            timeRange = "long_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//
//                    val recentlyPlayedDeferred = async(Dispatchers.IO) {
//                        spotifyRepositoryImpl.getRecentlyPlayedTracks(eTag = "", limit = GET_ITEM_NUM)
//                    }
//
//                    // Await all the deferred values
//                    val topArtistsShort = try {
//                        topArtistsDeferredShort.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//                    val topArtistsMedium = try {
//                        topArtistDeferredMedium.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//                    val topArtistsLong = try {
//                        topArtistsDeferredLong.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//
//                    val topTracksShort = try {
//                        topTracksDeferredShort.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//                    val topTracksMedium = try {
//                        topTracksDeferredMedium.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//                    val topTracksLong = try {
//                        topTracksDeferredLong.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//
//                    val recentlyPlayed = try {
//                        recentlyPlayedDeferred.await().items
//                    } catch (e: Exception) {
//                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
//                        throw e
//                    }
//
//
//                    _downLoadState.value = DownLoadState.Success(
//                        UserData(
//                            topArtistsShort = topArtistsShort,
//                            topArtistsMedium = topArtistsMedium,
//                            topArtistsLong = topArtistsLong,
//                            topTracksShort = topTracksShort,
//                            topTracksMedium = topTracksMedium,
//                            topTracksLong = topTracksLong,
//                            recentlyPlayed = recentlyPlayed
//                        )
//                    )
//                }
//            } catch (e: HttpException) {
//                _downLoadState.value = DownLoadState.Error(
//                    ErrorData(
//                        httpStatusCode = e.response()?.code(),
//                        errorMessage = e.message(),
//                        errorCause = e
//                    )
//                )
//                Log.e("SpotifyDataScreen", "HttpException: , ${e.response()?.code()}, $e, ${e.message}")
//            } catch (e: IOException) {
//                _downLoadState.value = DownLoadState.Error(
//                    ErrorData(
//                        httpStatusCode = null,
//                        errorMessage = "Network Error",
//                        errorCause = e
//                    )
//                )
//                Log.e("SpotifyDataScreen", "Network Error: , $e, ${e.message}")
//            } catch (e: Exception) {
//                _downLoadState.value = DownLoadState.Error(
//                    ErrorData(
//                        httpStatusCode = null,
//                        errorMessage = e.message,
//                        errorCause = e
//                    )
//                )
//                Log.e("SpotifyDataScreen", "Failed to load data", e)
//            }
//        }
    }
}