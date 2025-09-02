package com.example.geminispotifyapp.features.userdatadetail.recentlyplayed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.features.SnackbarMessage
import com.example.geminispotifyapp.features.UiEventManager
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper
import com.example.geminispotifyapp.features.userdatadetail.FetchResultWithEtag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: Could update recently played tracks by button.
@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val spotifyRepositoryImpl: SpotifyRepositoryImpl,
    private val apiExecutionHelper: ApiExecutionHelper,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResultWithEtag<List<PlayHistoryObject>>>(FetchResultWithEtag.Initial)
    val downLoadState: StateFlow<FetchResultWithEtag<List<PlayHistoryObject>>> = _downLoadState.asStateFlow()

    private val _displayedRecentlyPlayed = MutableStateFlow<List<PlayHistoryObject>>(emptyList())
    val displayedRecentlyPlayed: StateFlow<List<PlayHistoryObject>> = _displayedRecentlyPlayed.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasFetchedOnce = false
    private var eTag: String? = null

    fun fetchRecentlyPlayedIfNeeded() {
        if (hasFetchedOnce ||_downLoadState.value is FetchResultWithEtag.Loading) {
            return
        }
        Log.d("RecentlyPlayedViewModel", "Fetching recently played data...")

        _downLoadState.value = FetchResultWithEtag.Loading

        viewModelScope.launch {
            try {
                val result = fetchRecentlyPlayed()
                _downLoadState.value = result
                if (result is FetchResultWithEtag.Success) {
                    eTag = result.eTag
                    hasFetchedOnce = true
                    _displayedRecentlyPlayed.value = result.data
                    Log.d("RecentlyPlayedViewModel", "Recently played data fetched successfully.")
                }
            } catch (e: ApiError) {
                when (e) {
                    is ApiError.BadRequest -> Log.d(
                        "RecentlyPlayedViewModel",
                        "BadRequest: ${e.message}"
                    )

                    is ApiError.Forbidden -> Log.d("RecentlyPlayedViewModel", "Forbidden: ${e.message}")
                    is ApiError.HttpError -> Log.d("RecentlyPlayedViewModel", "HttpError: ${e.message}")
                    is ApiError.NetworkConnectionError -> {
                        Log.d(
                            "RecentlyPlayedViewModel",
                            "NetworkConnectionError: ${e.message}"
                        )
                        _downLoadState.value =
                            FetchResultWithEtag.Error(ApiError.NetworkConnectionError("Network connection error."))
                    }

                    is ApiError.NotFound -> Log.d("RecentlyPlayedViewModel", "NotFound: ${e.message}")
                    is ApiError.ServerError -> Log.d(
                        "RecentlyPlayedViewModel",
                        "ServerError: ${e.message}"
                    )

                    is ApiError.TooManyRequests -> Log.d(
                        "RecentlyPlayedViewModel",
                        "TooManyRequests: ${e.message}"
                    )

                    is ApiError.Unauthorized -> {
                        Log.d("RecentlyPlayedViewModel", "Unauthorized: ${e.message}")
                        spotifyRepositoryImpl.performLogOutAndCleanUp()
                        TODO() // Navigate to login screen.
                    }

                    else -> Log.d("RecentlyPlayedViewModel", "UnknownError of ApiError: ${e.message}")
                }
            } catch (e: Exception) {
                // 捕獲任何未被 ApiError 處理的、非預期的其他異常。
                // 這通常是您程式碼中的 bug 或預料之外的運行時問題。
                Log.e("RecentlyPlayedViewModel", "發生未預期錯誤: ${e.message}", e)
                _downLoadState.value =
                    FetchResultWithEtag.Error(ApiError.UnknownError("發生非預期錯誤，請稍後再試。"))
                // 同樣可以發送一個 SnackBar 提示，如果 GlobalUiEventPublisher 沒有處理這種通用異常的話
                // globalUiEventPublisher.publishMessage("發生非預期錯誤。")
            }
        }
    }

    fun reFetchRecentlyPlayedIfNeeded() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResultWithEtag.Initial
        fetchRecentlyPlayedIfNeeded()
    }

    // Only display snackbar message when refresh or load data failed (handled in ApiExecutionHelper).
    fun refreshRecentlyPlayed() {
        if (_isRefreshing.value || _downLoadState.value is FetchResultWithEtag.Loading) {
            return
        }
        Log.d("RecentlyPlayedViewModel", "Refreshing recently played data...")

        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val result = fetchRecentlyPlayed()
                if (result is FetchResultWithEtag.Success) {
                    eTag = result.eTag
                    _downLoadState.value = result
                    _displayedRecentlyPlayed.value = result.data
                    Log.d("RecentlyPlayedViewModel", "Recently played data refreshed successfully.")
                    Log.d("RecentlyPlayedViewModel", "eTag: $eTag")
                    uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Refresh successfully completed."))
                }
                else if (result is FetchResultWithEtag.NotModified) {
                    Log.d("RecentlyPlayedViewModel", "Recently played data is not modified.")
                    Log.d("RecentlyPlayedViewModel", "eTag: $eTag")
                    eTag = result.eTag
                    uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Data is not modified.")) // For TEST
                }
                else if (result is FetchResultWithEtag.Error) {
                    Log.e("RecentlyPlayedViewModel", "Failed to refresh recently played data.")
                }
                _isRefreshing.value = false
                Log.d("RecentlyPlayedViewModel", "Refresh completed. isRefreshing=$isRefreshing")
            } catch (e: ApiError) { // Normal ApiError won't make impact on current layout, only 401 or other non-Api error would.
                when (e) {
                    is ApiError.BadRequest -> Log.d(
                        "RecentlyPlayedViewModel",
                        "BadRequest: ${e.message}"
                    )

                    is ApiError.Forbidden -> Log.d("RecentlyPlayedViewModel", "Forbidden: ${e.message}")
                    is ApiError.HttpError -> Log.d("RecentlyPlayedViewModel", "HttpError: ${e.message}")
                    is ApiError.NetworkConnectionError -> Log.d(
                            "RecentlyPlayedViewModel",
                            "NetworkConnectionError: ${e.message}"
                        )
                    is ApiError.NotFound -> Log.d("RecentlyPlayedViewModel", "NotFound: ${e.message}")
                    is ApiError.ServerError -> Log.d(
                        "RecentlyPlayedViewModel",
                        "ServerError: ${e.message}"
                    )

                    is ApiError.TooManyRequests -> Log.d(
                        "RecentlyPlayedViewModel",
                        "TooManyRequests: ${e.message}"
                    )

                    is ApiError.Unauthorized -> {
                        Log.d("RecentlyPlayedViewModel", "Unauthorized: ${e.message}")
                        spotifyRepositoryImpl.performLogOutAndCleanUp()
                        TODO() // Navigate to login screen.
                    }

                    else -> Log.d("RecentlyPlayedViewModel", "UnknownError of ApiError: ${e.message}")
                }
            } catch (e: Exception) {
                // 捕獲任何未被 ApiError 處理的、非預期的其他異常。
                // 這通常是您程式碼中的 bug 或預料之外的運行時問題。
                Log.e("RecentlyPlayedViewModel", "發生未預期錯誤: ${e.message}", e)
                _downLoadState.value =
                    FetchResultWithEtag.Error(ApiError.UnknownError("發生非預期錯誤，請稍後再試。"))
                // 同樣可以發送一個 SnackBar 提示，如果 GlobalUiEventPublisher 沒有處理這種通用異常的話
                // globalUiEventPublisher.publishMessage("發生非預期錯誤。")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    private suspend fun fetchRecentlyPlayed(): FetchResultWithEtag<List<PlayHistoryObject>> {
        return apiExecutionHelper.executeEtaggedOperation(
            operation = {
                spotifyRepositoryImpl.getRecentlyPlayedTracks(limit = GET_ITEM_NUM, eTag = eTag)
            },
            transformSuccess = { response: RecentlyPlayedResponse ->
                response.items
            }
        )
    }
}