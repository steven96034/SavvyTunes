package com.example.geminispotifyapp.features.userdatadetail.toptracks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.data.SharedData
import com.example.geminispotifyapp.data.SpotifyTrack
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
    private val spotifyRepositoryImpl: SpotifyRepositoryImpl,
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
            try {
                val result = apiExecutionHelper.executeApiOperations(
                    operations = {
                        val topTracksDeferredShort = async(Dispatchers.IO) {
                            spotifyRepositoryImpl.getUserTopTracks(
                                timeRange = "short_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        val topTracksDeferredMedium = async(Dispatchers.IO) {
                            spotifyRepositoryImpl.getUserTopTracks(
                                timeRange = "medium_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        val topTracksDeferredLong = async(Dispatchers.IO) {
                            spotifyRepositoryImpl.getUserTopTracks(
                                timeRange = "long_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        // Return a list of Deferred, executeApiOperations will await them
                        listOf(
                            topTracksDeferredShort,
                            topTracksDeferredMedium,
                            topTracksDeferredLong
                        )
                    },
                    // transformSuccess lambda: Transform the List<SpotifyTracksResponse> to TopTrackData
                    transformSuccess = { results ->
                        // Results is a list of SpotifyTracksResponse, ensure type conversion is correct
                        val shortTermTracks =
                            results.getOrNull(0)?.items ?: emptyList()
                        val mediumTermTracks =
                            results.getOrNull(1)?.items ?: emptyList()
                        val longTermTracks =
                            results.getOrNull(2)?.items ?: emptyList()

                        TopTrackData(
                            topTracksShort = shortTermTracks,
                            topTracksMedium = mediumTermTracks,
                            topTracksLong = longTermTracks
                        )
                    }
                )
                // update UI status
                _downLoadState.value = result
            } catch (e: ApiError) {
                when (e) {
                    is ApiError.BadRequest -> Log.d(
                        "TopTracksViewModel",
                        "BadRequest: ${e.message}"
                    )

                    is ApiError.Forbidden -> Log.d("TopTracksViewModel", "Forbidden: ${e.message}")
                    is ApiError.HttpError -> Log.d("TopTracksViewModel", "HttpError: ${e.message}")
                    is ApiError.NetworkConnectionError -> Log.d(
                        "TopTracksViewModel",
                        "NetworkConnectionError: ${e.message}"
                    )

                    is ApiError.NotFound -> Log.d("TopTracksViewModel", "NotFound: ${e.message}")
                    is ApiError.ServerError -> Log.d(
                        "TopTracksViewModel",
                        "ServerError: ${e.message}"
                    )

                    is ApiError.TooManyRequests -> Log.d(
                        "TopTracksViewModel",
                        "TooManyRequests: ${e.message}"
                    )

                    is ApiError.Unauthorized -> {
                        Log.d("TopTracksViewModel", "Unauthorized: ${e.message}")
                        spotifyRepositoryImpl.performLogOutAndCleanUp()
                        TODO() // Navigate to login screen.
                    }

                    else -> Log.d("TopTracksViewModel", "UnknownError of ApiError: ${e.message}")
                }
            } catch (e: Exception) {
                // 捕獲任何未被 ApiError 處理的、非預期的其他異常。
                // 這通常是您程式碼中的 bug 或預料之外的運行時問題。
                Log.e("TopTracksViewModel", "發生未預期錯誤: ${e.message}", e)
                _downLoadState.value =
                    FetchResult.Error(ApiError.UnknownError("發生非預期錯誤，請稍後再試。"))
                // 同樣可以發送一個 SnackBar 提示，如果 GlobalUiEventPublisher 沒有處理這種通用異常的話
                // globalUiEventPublisher.publishMessage("發生非預期錯誤。")
            }
        }
    }
}




