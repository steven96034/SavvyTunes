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
import com.example.geminispotifyapp.features.userdatadetail.FetchResultWithEtag
import com.example.geminispotifyapp.features.userdatadetail.Period // Import Period enum
import com.example.geminispotifyapp.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TopArtistsData(
    val topArtistsShort: List<SpotifyArtist> = emptyList(),
    val topArtistsMedium: List<SpotifyArtist> = emptyList(),
    val topArtistsLong: List<SpotifyArtist> = emptyList(),
)

@HiltViewModel
class TopArtistsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<TopArtistsData>>(FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<TopArtistsData>> = _downLoadState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    private val _artistPeriodSelection = MutableStateFlow(Period.SHORT_TERM)
    val artistPeriodSelection: StateFlow<Period> = _artistPeriodSelection.asStateFlow()
    fun setArtistPeriodSelection(period: Period) {
        _artistPeriodSelection.value = period
    }
    val userDataNum: StateFlow<Int> = spotifyRepository.userDataNumFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )
    // In-memory storage for ETags
    private val etags = mutableMapOf<String, String?>(
        "short_term" to null,
        "medium_term" to null,
        "long_term" to null
    )
    // See if data is not modified
    private val notModified = mutableMapOf(
        "short_term" to false,
        "medium_term" to false,
        "long_term" to false
    )

    private var hasFetchedOnce = false
    private val tag = "TopArtistsViewModel"

        fun fetchTopArtistsIfNeeded() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Fetching top artists data for all periods...")

        _downLoadState.value = FetchResult.Loading
        viewModelScope.launch {
            // Fetch all three time ranges concurrently
            val allArtistResults = fetchAllArtistResultsConcurrently(null, true)
            val shortTermResult = allArtistResults[Period.SHORT_TERM]
            val mediumTermResult = allArtistResults[Period.MEDIUM_TERM]
            val longTermResult = allArtistResults[Period.LONG_TERM]

            // Aggregate results. If any fails, the whole fetch fails.
            if (shortTermResult is FetchResult.Error) {
                _downLoadState.value = shortTermResult
                handleApiError(shortTermResult.errorData)
                return@launch
            }
            if (mediumTermResult is FetchResult.Error) {
                _downLoadState.value = mediumTermResult
                handleApiError(mediumTermResult.errorData)
                return@launch
            }
            if (longTermResult is FetchResult.Error) {
                _downLoadState.value = longTermResult
                handleApiError(longTermResult.errorData)
                return@launch
            }

            val currentData = TopArtistsData(
                topArtistsShort = (shortTermResult as? FetchResult.Success)?.data?.topArtistsShort ?: emptyList(),
                topArtistsMedium = (mediumTermResult as? FetchResult.Success)?.data?.topArtistsMedium ?: emptyList(),
                topArtistsLong = (longTermResult as? FetchResult.Success)?.data?.topArtistsLong ?: emptyList(),
            )
            _downLoadState.value = FetchResult.Success(currentData)
            hasFetchedOnce = true
            Log.d(tag, "Top artists data fetched successfully.")
        }
    }

    fun reFetchTopArtist() {
        hasFetchedOnce = false
        _downLoadState.value = FetchResult.Initial
        fetchTopArtistsIfNeeded()
    }

    fun refreshTopArtists() {
        if (_isRefreshing.value) {
            return
        }
        Log.d(tag, "Refreshing top artists data...")
        _isRefreshing.value = true

        viewModelScope.launch {
            val currentArtistsData = (_downLoadState.value as? FetchResult.Success)?.data

            val allArtistResults = fetchAllArtistResultsConcurrently(etags)
            val shortTermResult = allArtistResults[Period.SHORT_TERM]
            val mediumTermResult = allArtistResults[Period.MEDIUM_TERM]
            val longTermResult = allArtistResults[Period.LONG_TERM]

            // Aggregate results. If any fails, the existing data should remain if available.
            val updatedShortArtists = when (shortTermResult) {
                is FetchResult.Success -> shortTermResult.data.topArtistsShort
                is FetchResult.Error -> currentArtistsData?.topArtistsShort ?: emptyList()
                else -> currentArtistsData?.topArtistsShort ?: emptyList()
            }
            val updatedMediumArtists = when (mediumTermResult) {
                is FetchResult.Success -> mediumTermResult.data.topArtistsMedium
                is FetchResult.Error -> currentArtistsData?.topArtistsMedium ?: emptyList()
                else -> currentArtistsData?.topArtistsMedium ?: emptyList()
            }
            val updatedLongArtists = when (longTermResult) {
                is FetchResult.Success -> longTermResult.data.topArtistsLong
                is FetchResult.Error -> currentArtistsData?.topArtistsLong ?: emptyList()
                else -> currentArtistsData?.topArtistsLong ?: emptyList()
            }

            val newTopArtistsData = TopArtistsData(
                topArtistsShort = updatedShortArtists,
                topArtistsMedium = updatedMediumArtists,
                topArtistsLong = updatedLongArtists,
            )

            _downLoadState.value = FetchResult.Success(newTopArtistsData)
            if (!notModified.values.all { it })
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Refresh successfully completed."))
            else {
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Refresh completed. (Data is not modified.)"))
            }

            _isRefreshing.value = false
            Log.d(tag, "Refresh completed. isRefreshing=${_isRefreshing.value}")

            // Handle errors for individual fetches during refresh
            if (shortTermResult is FetchResult.Error) handleApiError(shortTermResult.errorData)
            if (mediumTermResult is FetchResult.Error) handleApiError(mediumTermResult.errorData)
            if (longTermResult is FetchResult.Error) handleApiError(longTermResult.errorData)
        }
    }
    suspend fun fetchAllArtistResultsConcurrently(etags: Map<String, String?>?, forceRefresh: Boolean = false): Map<Period, FetchResult<TopArtistsData>> {
        return coroutineScope { // This scope ensures all async tasks complete
            val deferredResults = Period.entries.associateWith { timeRange ->
                async { // Launch each fetch operation in its own coroutine
                    fetchArtistsForTimeRange(timeRange.apiValue,
                        etags?.get(timeRange.apiValue), forceRefresh)
                }
            }
            // Await all results concurrently and combine them into a single map
            deferredResults.mapValues { (_, deferred) -> deferred.await() }
        }
    }
    private suspend fun fetchArtistsForTimeRange(timeRange: String, existingEtag: String?, forceRefresh: Boolean = false): FetchResult<TopArtistsData> {
        val currentEtag = if (forceRefresh) null else existingEtag
        Log.d(tag, "Fetching top artists for $timeRange, currentEtag=$currentEtag, forceRefresh=$forceRefresh")
        val result = spotifyRepository.getUserTopArtists(
            timeRange = timeRange,
            ifNoneMatch = currentEtag
        )

        return when (result) {
            is FetchResultWithEtag.Success -> {
                val parsedTimeRange = Period.fromString(timeRange)
                val currentData = (_downLoadState.value as? FetchResult.Success)?.data
                val baseData = currentData ?: TopArtistsData()
                val itemsToUpdate = result.data.items
                val newTopArtistsData = when (parsedTimeRange) {
                    Period.SHORT_TERM -> baseData.copy(topArtistsShort = itemsToUpdate)
                    Period.MEDIUM_TERM -> baseData.copy(topArtistsMedium = itemsToUpdate)
                    Period.LONG_TERM -> baseData.copy(topArtistsLong = itemsToUpdate)
                    else -> {
                        // Should not happen
                        Log.e(tag, "Received unknown timeRange string: '$timeRange'. Returning existing data.")
                        TopArtistsData()
                    }
                }
                etags[timeRange] = result.eTag
                notModified[timeRange] = false
                Log.d(tag, "Top artists eTag for $timeRange: ${result.eTag}")
                FetchResult.Success(newTopArtistsData)
            }
            is FetchResultWithEtag.NotModified -> {
                Log.d(tag, "Top artists for $timeRange not modified.")
                notModified[timeRange] = true
                val currentData = (_downLoadState.value as? FetchResult.Success)?.data
                if (currentData != null) {
                    FetchResult.Success(currentData)
                } else {
                    // If data is NotModified but we don't have any existing data, this is an error.
                    // This case should ideally not happen if Initial fetch populates data.
                    FetchResult.Error(ApiError.UnknownError("Data not modified but no local data available."))
                }
            }
            is FetchResultWithEtag.Error -> {
                handleApiError(result.errorData)
                notModified[timeRange] = false
                FetchResult.Error(result.errorData)
            }
            FetchResultWithEtag.Initial -> FetchResult.Initial // Should not happen
            FetchResultWithEtag.Loading -> FetchResult.Loading // Should not happen
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
                is UiEvent.ShowSnackbarWithAction -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction(uiEvent.message, uiEvent.actionLabel))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.Unauthorized(uiEvent.message))
                }
            }
        }
    }
}