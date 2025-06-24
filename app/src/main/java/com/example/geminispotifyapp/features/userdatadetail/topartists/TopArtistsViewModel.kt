package com.example.geminispotifyapp.features.userdatadetail.topartists

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SharedData
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.TopArtistsResponse
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
    private val apiExecutionHelper: ApiExecutionHelper
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopArtistsData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopArtistsData>> = _downLoadState.asStateFlow()

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
                    listOf(topArtistsDeferredShort, topArtistsDeferredMedium, topTracksDeferredLong)
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
        }
    }
}