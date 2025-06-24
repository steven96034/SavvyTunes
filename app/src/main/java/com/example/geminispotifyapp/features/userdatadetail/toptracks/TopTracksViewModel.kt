package com.example.geminispotifyapp.features.userdatadetail.toptracks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SharedData
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopTrackData(
    val topTracksShort: List<SpotifyTrack> = emptyList(),
    val topTracksMedium: List<SpotifyTrack> = emptyList(),
    val topTracksLong: List<SpotifyTrack> = emptyList()
)

// TODO: Could update top artists data by button.
@HiltViewModel
class TopTracksViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopTrackData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopTrackData>> = _downLoadState.asStateFlow()

//    init {
//        fetchTopTracks()
//    }
    private var hasFetchedOnce = false

    // Define a function to fetch data
    fun fetchTopTracks() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d("TopTracksViewModel", "Fetching top tracks data...")

        hasFetchedOnce = true
        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = apiExecutionHelper.executeApiOperations(
                operations = {
                    val topTracksDeferredShort = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "short_term",
                            limit = SharedData.GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredMedium = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "medium_term",
                            limit = SharedData.GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredLong = async(Dispatchers.IO) {
                        spotifyRepository.getUserTopTracks(
                            timeRange = "long_term",
                            limit = SharedData.GET_ITEM_NUM
                        )
                    }
                    // Return a list of Deferred, executeApiOperations will await them
                    listOf(topTracksDeferredShort, topTracksDeferredMedium, topTracksDeferredLong)
                },
                // transformSuccess lambda: Transform the List<SpotifyTracksResponse> to TopTrackData
                transformSuccess = { results ->
                    // Results is a list of SpotifyTracksResponse, ensure type conversion is correct
                    val shortTermTracks =
                        (results.getOrNull(0) as? TopTracksResponse)?.items ?: emptyList()
                    val mediumTermTracks =
                        (results.getOrNull(1) as? TopTracksResponse)?.items ?: emptyList()
                    val longTermTracks =
                        (results.getOrNull(2) as? TopTracksResponse)?.items ?: emptyList()

                    TopTrackData(
                        topTracksShort = shortTermTracks,
                        topTracksMedium = mediumTermTracks,
                        topTracksLong = longTermTracks
                    )
                }
            )
            // update UI status
            _downLoadState.value = result


//        if (_downLoadState.value is DownLoadState.Loading) { // || _downLoadState.value is DownLoadState.Success
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
//                    // Use async to fetch data in parallel
//
//                    val topTracksDeferredShort = async(Dispatchers.IO) {
//                        spotifyRepository.getUserTopTracks(
//                            timeRange = "short_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topTracksDeferredMedium = async(Dispatchers.IO) {
//                        spotifyRepository.getUserTopTracks(
//                            timeRange = "medium_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//                    val topTracksDeferredLong = async(Dispatchers.IO) {
//                        spotifyRepository.getUserTopTracks(
//                            timeRange = "long_term",
//                            limit = GET_ITEM_NUM
//                        )
//                    }
//
//                    // Await all the deferred values
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
//
//                    _downLoadState.value = DownLoadState.Success(
//                        TopTrackData(
//                            topTracksShort = topTracksShort,
//                            topTracksMedium = topTracksMedium,
//                            topTracksLong = topTracksLong,
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
//                Log.e("TopTracksViewModel", "HttpException: , ${e.response()?.code()}, $e, ${e.message}")
//                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
//            } catch (e: IOException) {
//                _downLoadState.value = DownLoadState.Error(
//                    ErrorData(
//                        httpStatusCode = null,
//                        errorMessage = "Network Error",
//                        errorCause = e
//                    )
//                )
//                Log.e("TopTracksViewModel", "Network Error: , $e, ${e.message}")
//                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
//            } catch (e: Exception) {
//                _downLoadState.value = DownLoadState.Error(
//                    ErrorData(
//                        httpStatusCode = null,
//                        errorMessage = e.message,
//                        errorCause = e
//                    )
//                )
//                Log.e("TopTracksViewModel", "Failed to load data", e)
//                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
//            }
//        }
        }

//    sealed interface DownLoadState {
//        data object Initial : DownLoadState
//        data object Loading : DownLoadState
//        data class Success(val data: TopTrackData) : DownLoadState
//        data class Error(val data: ErrorData) : DownLoadState
//    }

    }
}




