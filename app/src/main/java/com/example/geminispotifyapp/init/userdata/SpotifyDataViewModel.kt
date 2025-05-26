package com.example.geminispotifyapp.init.userdata

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.DownLoadState
import com.example.geminispotifyapp.ErrorData
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.UserData
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SpotifyDataViewModel @Inject constructor(private val spotifyRepository: SpotifyRepository) : ViewModel() {

    private val _downLoadState: MutableStateFlow<DownLoadState> = MutableStateFlow(DownLoadState.Initial)
    val downLoadState: StateFlow<DownLoadState> = _downLoadState


    // Define a function to fetch data
    fun fetchData() {
        if (_downLoadState.value is DownLoadState.Loading || _downLoadState.value is DownLoadState.Success) {
            return
        }

        _downLoadState.value = DownLoadState.Loading

        viewModelScope.launch {
            try {
                // Use supervisorScope to ensure that a single task fails doesn't cancel other tasks
                supervisorScope {
                    Log.d("SpotifyDataScreen", "Fetching data...")

                    // Use async to fetch data in parallel
                    val topArtistsDeferredShort = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopArtists(
                            timeRange = "short_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topArtistDeferredMedium = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopArtists(
                            timeRange = "medium_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topArtistsDeferredLong = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopArtists(
                            timeRange = "long_term",
                            limit = GET_ITEM_NUM
                        )
                    }

                    val topTracksDeferredShort = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "short_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredMedium = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "medium_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredLong = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "long_term",
                            limit = GET_ITEM_NUM
                        )
                    }

                    val recentlyPlayedDeferred = async(Dispatchers.IO) {
                        spotifyRepository.getRecentlyPlayedTracks(limit = GET_ITEM_NUM)
                    }

                    // Await all the deferred values
                    val topArtistsShort = try {
                        topArtistsDeferredShort.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topArtistsMedium = try {
                        topArtistDeferredMedium.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topArtistsLong = try {
                        topArtistsDeferredLong.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }

                    val topTracksShort = try {
                        topTracksDeferredShort.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topTracksMedium = try {
                        topTracksDeferredMedium.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topTracksLong = try {
                        topTracksDeferredLong.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }

                    val recentlyPlayed = try {
                        recentlyPlayedDeferred.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }


                    _downLoadState.value = DownLoadState.Success(
                        UserData(
                            topArtistsShort = topArtistsShort,
                            topArtistsMedium = topArtistsMedium,
                            topArtistsLong = topArtistsLong,
                            topTracksShort = topTracksShort,
                            topTracksMedium = topTracksMedium,
                            topTracksLong = topTracksLong,
                            recentlyPlayed = recentlyPlayed
                        )
                    )
                }
            } catch (e: HttpException) {
                _downLoadState.value = DownLoadState.Error(
                    ErrorData(
                        httpStatusCode = e.response()?.code(),
                        errorMessage = e.message(),
                        errorCause = e
                    )
                )
                Log.e("SpotifyDataScreen", "HttpException: , ${e.response()?.code()}, $e, ${e.message}")
            } catch (e: IOException) {
                _downLoadState.value = DownLoadState.Error(
                    ErrorData(
                        httpStatusCode = null,
                        errorMessage = "Network Error",
                        errorCause = e
                    )
                )
                Log.e("SpotifyDataScreen", "Network Error: , $e, ${e.message}")
            } catch (e: Exception) {
                _downLoadState.value = DownLoadState.Error(
                    ErrorData(
                        httpStatusCode = null,
                        errorMessage = e.message,
                        errorCause = e
                    )
                )
                Log.e("SpotifyDataScreen", "Failed to load data", e)
            }
        }
    }
}