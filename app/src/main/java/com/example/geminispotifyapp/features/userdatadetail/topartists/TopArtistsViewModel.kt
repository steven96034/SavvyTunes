package com.example.geminispotifyapp.features.userdatadetail.topartists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
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
// TODO: Could update top artists data by button.
@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    //private val apiExecutionHelper: ApiExecutionHelper,
    private val getTopArtistsUseCase: GetTopArtistsUseCase
    //private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopArtistsData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopArtistsData>> = _downLoadState.asStateFlow()

    //val snackBarEvent = uiEventManager.snackbarEvent

//    init {
//        fetchTopArtists()
//    }
    private var hasFetchedOnce = false

    fun reFetchTopArtist() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResult.Initial
        fetchTopArtists()
    }


    fun fetchTopArtists() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d("TopArtistsViewModel", "Fetching top artists data...")

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

    private suspend fun handleApiError(error: ApiError) {
        when (error) {
            is ApiError.BadRequest -> Log.d(
                "TopArtistsViewModel",
                "BadRequest: ${error.message}"
            )

            is ApiError.Forbidden -> Log.d("TopArtistsViewModel", "Forbidden: ${error.message}")
            is ApiError.HttpError -> Log.d("TopArtistsViewModel", "HttpError: ${error.message}")
            is ApiError.NetworkConnectionError -> {
                Log.d(
                    "TopArtistsViewModel",
                    "NetworkConnectionError: ${error.message}"
                )
                _downLoadState.value =
                    FetchResult.Error(ApiError.NetworkConnectionError("Network connection error."))
            }

            is ApiError.NotFound -> Log.d("TopArtistsViewModel", "NotFound: ${error.message}")
            is ApiError.ServerError -> Log.d(
                "TopArtistsViewModel",
                "ServerError: ${error.message}"
            )

            is ApiError.TooManyRequests -> Log.d(
                "TopArtistsViewModel",
                "TooManyRequests: ${error.message}"
            )

            is ApiError.Unauthorized -> {
                Log.d("TopArtistsViewModel", "Unauthorized: ${error.message}")
                spotifyRepository.performLogOutAndCleanUp()
                TODO() // Navigate to login screen.
            }

            else -> {
                Log.d("TopArtistsViewModel", "UnknownError of ApiError: ${error.message}")
                _downLoadState.value =
                    FetchResult.Error(ApiError.UnknownError("UnknownError, please try again later."))
            }
        }
    }
}