package com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.PlayHistoryObject
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.core.utils.GlobalErrorHandler
import com.example.geminispotifyapp.data.debug.AppConfig.isMockMode
import com.example.geminispotifyapp.data.debug.MockData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class UiPlayHistoryObject(
    val originalPlayHistory: PlayHistoryObject,
    val formattedPlayedAtTimeAgo: String,
    val formattedPlayedAtDateTime: String
)

@HiltViewModel
class RecentlyPlayedViewModel @Inject constructor(
    spotifyRepository: SpotifyRepository,
    private val getRecentlyPlayedUseCase: GetRecentlyPlayedUseCase,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val _downLoadState = MutableStateFlow<FetchResult<List<UiPlayHistoryObject>>>(
        FetchResult.Initial)
    val downLoadState: StateFlow<FetchResult<List<UiPlayHistoryObject>>> = _downLoadState.asStateFlow()

    private val _displayedRecentlyPlayed = MutableStateFlow<List<UiPlayHistoryObject>>(emptyList())
    val displayedRecentlyPlayed: StateFlow<List<UiPlayHistoryObject>> = _displayedRecentlyPlayed.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val userDataNum: StateFlow<Int> = spotifyRepository.userDataNumFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )
    private var hasFetchedOnce = false
    private val tag = "RecentlyPlayedViewModel"

    fun fetchRecentlyPlayedIfNeeded() {
        if (hasFetchedOnce || _downLoadState.value is FetchResult.Loading) {
            return
        }
        Log.d(tag, "Fetching recently played data...")

        if (isMockMode) {
            viewModelScope.launch {
                _downLoadState.value = FetchResult.Loading
                delay(1000)
                _downLoadState.value = FetchResult.Success(MockData.mockUiPlayHistoryObjects)
                _displayedRecentlyPlayed.value = MockData.mockUiPlayHistoryObjects
                hasFetchedOnce = true
            }
            return
        }

        _downLoadState.value = FetchResult.Loading

        viewModelScope.launch {
            val result = getRecentlyPlayedUseCase()
            _downLoadState.value = when (result) {
                is FetchResult.Success -> {
                    hasFetchedOnce = true
                    val uiPlayHistoryObjects = result.data.map { history ->
                        UiPlayHistoryObject(
                            originalPlayHistory = history,
                            formattedPlayedAtTimeAgo = formatTimeAgo(history.playedAt),
                            formattedPlayedAtDateTime = formatTime(history.playedAt)
                        )
                    }
                    _displayedRecentlyPlayed.value = uiPlayHistoryObjects
                    Log.d(tag, "Recently played data fetched successfully.")
                    FetchResult.Success(uiPlayHistoryObjects)
                }
                is FetchResult.Error -> {
                    handleApiError(result.errorData)
                    result
                }
                FetchResult.Initial -> FetchResult.Initial // Should not happen after use case execution
                FetchResult.Loading -> FetchResult.Loading // Should not happen after use case execution
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

        if (isMockMode) {
            viewModelScope.launch {
                _isRefreshing.value = true
                _downLoadState.value = FetchResult.Loading
                delay(1000)
                _downLoadState.value = FetchResult.Success(MockData.mockUiPlayHistoryObjects)
                _displayedRecentlyPlayed.value = MockData.mockUiPlayHistoryObjects
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Refresh successfully completed."))
                _isRefreshing.value = false
            }
            return
        }

        viewModelScope.launch {
            val currentData = (_downLoadState.value as? FetchResult.Success)?.data
            _isRefreshing.value = true
            val result = getRecentlyPlayedUseCase()
            _downLoadState.value = when (result) {
                is FetchResult.Success -> {
                    val uiPlayHistoryObjects = result.data.map { history ->
                        UiPlayHistoryObject(
                            originalPlayHistory = history,
                            formattedPlayedAtTimeAgo = formatTimeAgo(history.playedAt),
                            formattedPlayedAtDateTime = formatTime(history.playedAt)
                        )
                    }
                    _displayedRecentlyPlayed.value = uiPlayHistoryObjects
                    Log.d(tag, "Recently played data refreshed successfully.")
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar("Refresh successfully completed."))
                    FetchResult.Success(uiPlayHistoryObjects)
                }
                is FetchResult.Error -> {
                    handleApiError(result.errorData)
                    Log.e(tag, "Failed to refresh recently played data.")
                    if (currentData != null)
                        FetchResult.Success(currentData)
                    else
                        result
                }
                FetchResult.Initial -> FetchResult.Initial
                FetchResult.Loading -> FetchResult.Loading
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
                is UiEvent.ShowSnackbarWithAction -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction(uiEvent.message, uiEvent.actionLabel))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.Unauthorized(uiEvent.message))
                }
                else -> {}
            }
        }
    }

    internal fun formatTimeAgo(dateString: String): String {
        return try {
            val instant = Instant.parse(dateString)
            val now = Instant.now()
            val duration = Duration.between(instant, now)

            when {
                duration.toMinutes() < 1 -> "Just Now"
                duration.toHours() < 1 -> "${duration.toMinutes()} minutes ago"
                duration.toDays() < 1 -> "${duration.toHours()} hours ago"
                else -> "${duration.toDays()} days ago"
            }
        } catch (e: DateTimeParseException) {
            Log.e("TimeFormat", "Fail to parse date with java.time.Instant: $dateString", e)
            "Unknown Time"
        } catch (e: Exception) {
            Log.e("TimeFormat", "An unexpected error occurred in formatTimeAgo: $dateString", e)
            "Unknown Time"
        }
    }

    internal fun formatTime(dateString: String): String {
        return try {
            val instant = Instant.parse(dateString)
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())
            formatter.format(instant)
        } catch (e: DateTimeParseException) {
            Log.e("TimeFormat", "Fail to parse date with java.time.Instant: $dateString", e)
            "Unknown Time"
        } catch (e: Exception) {
            Log.e("TimeFormat", "An unexpected error occurred in formatTime: $dateString", e)
            "Unknown Time"
        }
    }
}