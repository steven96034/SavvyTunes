package com.example.geminispotifyapp.features.userdatadetail.recentlyplayed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.features.SnackbarMessage
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

// TODO: Could update recently played tracks by button.
@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<List<PlayHistoryObject>>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<List<PlayHistoryObject>>> = _downLoadState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasFetchedOnce = false

    fun fetchRecentlyPlayedIfNeeded() {
        if (hasFetchedOnce ||_downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d("RecentlyPlayedViewModel", "Fetching recently played data...")

        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = fetchRecentlyPlayed()
            _downLoadState.value = result
            if (result is FetchResult.Success) {
                hasFetchedOnce = true
                Log.d("RecentlyPlayedViewModel", "Recently played data fetched successfully.")
            }
        }
    }

    // Only display snackbar message when refresh or load data failed (handled in ApiExecutionHelper).
    fun refreshRecentlyPlayed() {
        if (_isRefreshing.value || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d("RecentlyPlayedViewModel", "Refreshing recently played data...")

        viewModelScope.launch {
            _isRefreshing.value = true
            val result = fetchRecentlyPlayed()
            if (result is FetchResult.Success) {
                _downLoadState.value = result
                Log.d("RecentlyPlayedViewModel", "Recently played data refreshed successfully.")
                uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Refresh successfully completed."))
            } else if (result is FetchResult.Error) {
                Log.e("RecentlyPlayedViewModel", "Failed to refresh recently played data.")
            }
            _isRefreshing.value = false
            Log.d("RecentlyPlayedViewModel", "Refresh completed. isRefreshing=$isRefreshing")
        }
    }

    suspend fun fetchRecentlyPlayed(): FetchResult<List<PlayHistoryObject>> {
        return apiExecutionHelper.executeApiOperations(
            operations = {
                val recentlyPlayedDeferred = async(Dispatchers.IO) {
                    spotifyRepository.getRecentlyPlayedTracks(
                        limit = GET_ITEM_NUM
                    )
                }
                // Return a list of Deferred, executeApiOperations will await them
                listOf(recentlyPlayedDeferred)
            },
            // transformSuccess lambda: Transform the List<RecentlyPlayedResponse> to TopArtistData
            transformSuccess = { results ->
                // Results is a list of RecentlyPlayedResponse, ensure type conversion is correct
                val recentlyPlayed =
                    (results.getOrNull(0) as? RecentlyPlayedResponse)?.items ?: emptyList()

                recentlyPlayed
            }
        )
        //return result
    }
}