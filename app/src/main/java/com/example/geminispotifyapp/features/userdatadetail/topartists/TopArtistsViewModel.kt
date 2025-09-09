package com.example.geminispotifyapp.features.userdatadetail.topartists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyArtist
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

data class TopArtistsData(
    val topArtistsShort: List<SpotifyArtist> = emptyList(),
    val topArtistsMedium: List<SpotifyArtist> = emptyList(),
    val topArtistsLong: List<SpotifyArtist> = emptyList()
)
@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val getTopArtistsUseCase: GetTopArtistsUseCase,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopArtistsData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopArtistsData>> = _downLoadState.asStateFlow()

    //val snackBarEvent = uiEventManager.snackbarEvent

//    init {
//        fetchTopArtists()
//    }
    private var hasFetchedOnce = false
    private val tag = "TopArtistsViewModel"

    fun reFetchTopArtist() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResult.Initial
        fetchTopArtists()
    }


    fun fetchTopArtists() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Fetching top artists data...")

        hasFetchedOnce = true
        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = getTopArtistsUseCase()
            _downLoadState.value = result

            if (result is FetchResult.Error) {
                handleApiError(result.errorData)
            }
        }
    }

    fun testSnackbar() {
        viewModelScope.launch {
            Log.d(tag, "testSnackbar() called, emitting event...")
            uiEventManager.sendEvent(UiEvent.ShowSnackbar("這是一則測試訊息"))
            Log.d(tag, "testSnackbar() event emitted.")
        }
    }

    private fun handleApiError(error: ApiError) {
        viewModelScope.launch {
            val uiEvent = globalErrorHandler.processError(error, tag)

            // According to the returned UiAction, use the sendEvent function of the inherited sendEvent function to send events
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