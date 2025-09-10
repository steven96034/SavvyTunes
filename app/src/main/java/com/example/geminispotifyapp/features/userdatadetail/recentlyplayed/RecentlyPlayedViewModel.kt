package com.example.geminispotifyapp.features.userdatadetail.recentlyplayed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.features.UiEventManager
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val getRecentlyPlayedUseCase: GetRecentlyPlayedUseCase,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<List<PlayHistoryObject>>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<List<PlayHistoryObject>>> = _downLoadState.asStateFlow()

    private val _displayedRecentlyPlayed = MutableStateFlow<List<PlayHistoryObject>>(emptyList())
    val displayedRecentlyPlayed: StateFlow<List<PlayHistoryObject>> = _displayedRecentlyPlayed.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var hasFetchedOnce = false
    private val tag = "RecentlyPlayedViewModel"

    fun fetchRecentlyPlayedIfNeeded() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Fetching recently played data...")

        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = getRecentlyPlayedUseCase()
            _downLoadState.value = result

            if (result is FetchResult.Success) {
                hasFetchedOnce = true
                _displayedRecentlyPlayed.value = result.data
                Log.d(tag, "Recently played data fetched successfully.")
            } else if (result is FetchResult.Error) {
                handleApiError(result.errorData)
            }
        }
    }

    fun reFetchRecentlyPlayedIfNeeded() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResult.Initial
        fetchRecentlyPlayedIfNeeded()
    }

    fun refreshRecentlyPlayed() {
        if (_isRefreshing.value || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Refreshing recently played data...")

        viewModelScope.launch {
            _isRefreshing.value = true
            val result = getRecentlyPlayedUseCase()
            _downLoadState.value = result

            if (result is FetchResult.Success) {
                _displayedRecentlyPlayed.value = result.data
                Log.d(tag, "Recently played data refreshed successfully.")
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Refresh successfully completed."))
            } else if (result is FetchResult.Error) {
                handleApiError(result.errorData)
                Log.e(tag, "Failed to refresh recently played data.")
            }
            _isRefreshing.value = false
            Log.d(tag, "Refresh completed. isRefreshing=${_isRefreshing.value}")
        }
    }

    private fun handleApiError(error: ApiError) {
        viewModelScope.launch {
            val uiEvent = globalErrorHandler.processError(error, tag)

            when (uiEvent) {
                is UiEvent.ShowSnackbar -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar(uiEvent.message))
                }
                is UiEvent.ShowSnackbarDetail -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarDetail(uiEvent.message, uiEvent.detail))
                }
                is UiEvent.Navigate -> {
                    uiEventManager.sendEvent(UiEvent.Navigate(uiEvent.route))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar(uiEvent.message))
                    uiEventManager.sendEvent(UiEvent.Navigate(uiEvent.navigationRoute))
                }
            }
        }
    }
}