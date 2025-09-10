package com.example.geminispotifyapp.features.userdatadetail.toptracks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyTrack
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

data class TopTrackData(
    val topTracksShort: List<SpotifyTrack> = emptyList(),
    val topTracksMedium: List<SpotifyTrack> = emptyList(),
    val topTracksLong: List<SpotifyTrack> = emptyList()
)

@HiltViewModel
class TopTracksViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val getTopTracksUseCase: GetTopTracksUseCase,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopTrackData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopTrackData>> = _downLoadState.asStateFlow()

    private var hasFetchedOnce = false
    private val tag = "TopTracksViewModel"

    fun reFetchTopTrack() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResult.Initial
        fetchTopTracks()
    }

    fun fetchTopTracks() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Fetching top tracks data...")

        hasFetchedOnce = true
        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = getTopTracksUseCase()
            _downLoadState.value = result

            if (result is FetchResult.Error) {
                handleApiError(result.errorData)
            }
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