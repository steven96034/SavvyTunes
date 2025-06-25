package com.example.geminispotifyapp.features.userdatadetail.topartists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SharedData
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.TopArtistsResponse
import com.example.geminispotifyapp.features.UiEventManager
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

data class TopArtistsData(
    val topArtistsShort: List<SpotifyArtist> = emptyList(),
    val topArtistsMedium: List<SpotifyArtist> = emptyList(),
    val topArtistsLong: List<SpotifyArtist> = emptyList()
)
// TODO: Could update top artists data by button.
@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper,
    //private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopArtistsData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopArtistsData>> = _downLoadState.asStateFlow()

    //val snackBarEvent = uiEventManager.snackbarEvent

//    init {
//        fetchTopArtists()
//    }
    private var hasFetchedOnce = false


    fun fetchTopArtists() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d("TopArtistsViewModel", "Fetching top artists data...")

        hasFetchedOnce = true
        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            try {
                val result = apiExecutionHelper.executeApiOperations(
                    operations = {
                        val topArtistsDeferredShort = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "short_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        val topArtistsDeferredMedium = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "medium_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        val topTracksDeferredLong = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "long_term",
                                limit = SharedData.GET_ITEM_NUM
                            )
                        }
                        // Return a list of Deferred, executeApiOperations will await them
                        listOf(
                            topArtistsDeferredShort,
                            topArtistsDeferredMedium,
                            topTracksDeferredLong
                        )
                    },
                    // transformSuccess lambda: Transform the List<SpotifyArtistsResponse> to TopArtistData
                    transformSuccess = { results ->
                        // Results is a list of SpotifyArtistsResponse, ensure type conversion is correct
                        val shortTermArtist =
                            (results.getOrNull(0) as? TopArtistsResponse)?.items ?: emptyList()
                        val mediumTermArtists =
                            (results.getOrNull(1) as? TopArtistsResponse)?.items ?: emptyList()
                        val longTermArtists =
                            (results.getOrNull(2) as? TopArtistsResponse)?.items ?: emptyList()

                        TopArtistsData(
                            topArtistsShort = shortTermArtist,
                            topArtistsMedium = mediumTermArtists,
                            topArtistsLong = longTermArtists
                        )
                    }
                )
                // update UI status
                _downLoadState.value = result
            } catch (e: ApiError) {
                when (e) {
                    is ApiError.BadRequest -> Log.d(
                        "TopArtistsViewModel",
                        "BadRequest: ${e.message}"
                    )

                    is ApiError.Forbidden -> Log.d("TopArtistsViewModel", "Forbidden: ${e.message}")
                    is ApiError.HttpError -> Log.d("TopArtistsViewModel", "HttpError: ${e.message}")
                    is ApiError.NetworkConnectionError -> Log.d(
                        "TopArtistsViewModel",
                        "NetworkConnectionError: ${e.message}"
                    )

                    is ApiError.NotFound -> Log.d("TopArtistsViewModel", "NotFound: ${e.message}")
                    is ApiError.ServerError -> Log.d(
                        "TopArtistsViewModel",
                        "ServerError: ${e.message}"
                    )

                    is ApiError.TooManyRequests -> Log.d(
                        "TopArtistsViewModel",
                        "TooManyRequests: ${e.message}"
                    )

                    is ApiError.Unauthorized -> {
                        Log.d("TopArtistsViewModel", "Unauthorized: ${e.message}")
                        spotifyRepository.performLogOutAndCleanUp()
                        TODO() // Navigate to login screen.
                    }

                    else -> Log.d("TopArtistsViewModel", "UnknownError of ApiError: ${e.message}")
                }
            } catch (e: Exception) {
                // 捕獲任何未被 ApiError 處理的、非預期的其他異常。
                // 這通常是您程式碼中的 bug 或預料之外的運行時問題。
                Log.e("TopArtistsViewModel", "發生未預期錯誤: ${e.message}", e)
                _downLoadState.value =
                    FetchResult.Error(ApiError.UnknownError("發生非預期錯誤，請稍後再試。"))
                // 同樣可以發送一個 SnackBar 提示，如果 GlobalUiEventPublisher 沒有處理這種通用異常的話
                // globalUiEventPublisher.publishMessage("發生非預期錯誤。")
            }
        }
    }
}