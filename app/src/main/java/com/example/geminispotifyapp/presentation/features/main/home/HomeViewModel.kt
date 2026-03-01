package com.example.geminispotifyapp.presentation.features.main.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.core.utils.GlobalErrorHandler
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.data.debug.AppConfig.isMockMode
import com.example.geminispotifyapp.data.debug.MockData
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.data.remote.model.WeeklyRecommendation
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.domain.repository.WeatherDataRepository
import com.example.geminispotifyapp.domain.repository.WeatherIconRepository
import com.example.geminispotifyapp.domain.usecase.FindWeatherRelatedMusic
import com.example.geminispotifyapp.domain.usecase.GetLocationAndWeatherUseCase
import com.example.geminispotifyapp.presentation.MainScreen
import com.example.geminispotifyapp.presentation.SettingsScreen
import com.google.ai.client.generativeai.type.ServerException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class TwoTracksList(
    val tracksA: List<SpotifyTrack>?,
    val tracksB: List<SpotifyTrack>?
)

data class HomeDataPayload(
    val location: Location,
    val weatherResponse: WeatherResponse
)

sealed class HomeDataState {
    object Initial : HomeDataState()
    object Loading : HomeDataState()
    data class Success(val data: HomeDataPayload) : HomeDataState()
    data class Error(val message: String) : HomeDataState()
    object GpsDisabled : HomeDataState()
    object MissingPermission: HomeDataState()
}

sealed interface RecommendationUiState {
    data object Loading : RecommendationUiState
    data object Empty : RecommendationUiState
    data class Success(val data: WeeklyRecommendation) : RecommendationUiState
    data class Error(val message: String) : RecommendationUiState
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val uiEventManager: UiEventManager,
    private val findWeatherRelatedMusicUseCase: FindWeatherRelatedMusic,
    val weatherIconRepository: WeatherIconRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val getLocationAndWeatherUseCase: GetLocationAndWeatherUseCase,
    private val spotifyRepository: SpotifyRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val globalErrorHandler: GlobalErrorHandler
): ViewModel() {
    private val tag = "HomeViewModel"
    private val _weatherDataJson = MutableStateFlow<WeatherResponse?>(null)
    val weatherDataJson: StateFlow<WeatherResponse?> = _weatherDataJson.asStateFlow()

    private val _showGpsDialog = MutableStateFlow(false)
    val showGpsDialog: StateFlow<Boolean> = _showGpsDialog

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _recommendationUiState = MutableStateFlow<RecommendationUiState>(RecommendationUiState.Loading)
    val recommendationUiState = _recommendationUiState.asStateFlow()

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

    val isNotificationPromptDismissed: StateFlow<Boolean> = spotifyRepository.isNotificationPromptDismissedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    fun dismissNotificationPrompt() {
        viewModelScope.launch {
            spotifyRepository.setNotificationPromptDismissed(true)
        }
    }

    fun setBottomSheetVisibility(isVisible: Boolean) {
        _showBottomSheet.value = isVisible
    }

    init {
        viewModelScope.launch {
            // Ensure that all launched child coroutines complete before the current coroutine ends.
            coroutineScope {
                // Do not wait for the result of the tasks.
                launch { checkIfEverydayRecommendationSeenAndTriggerModalBottomSheet() }
                launch { testTokensIfValid() }
                launch { observeJsonWeatherData() }
            }
        }
    }

    // Test if the refresh/access tokens are valid by fetching user profile.
    private suspend fun testTokensIfValid() {
        val result = spotifyRepository.getUserProfile()
        if (result is FetchResult.Success) {
            Log.d(tag, "Tokens Valid: ${result.data}")
        } else if (result is FetchResult.Error) {
            handleApiError(result.errorData)
            Log.d(tag, "Tokens Error: ${result.errorData}")
        }
    }

    private suspend fun observeJsonWeatherData() {
        weatherDataRepository.weatherData.collect { weatherResponse ->
            _weatherDataJson.value = weatherResponse
            Log.d(tag, "JSON Weather Data Updated: $weatherResponse")
        }
    }

    val refreshTag = "RefreshHomeData"
    fun refreshHome() {
        if (_isRefreshing.value) {
            return
        }

        _isRefreshing.value = true

        val weatherData = _weatherDataJson.value
        if (weatherData != null) { // The data in cache is valid.
            val dataTimeStr = weatherData.current.time
            val pattern = "yyyy-MM-dd  HH:mm"
            val formatter = DateTimeFormatter.ofPattern(pattern)
            lateinit var localDateTime: LocalDateTime
            try {
                localDateTime = LocalDateTime.parse(dataTimeStr, formatter)
                Log.d(refreshTag, "Weather data time(Before/After): $dataTimeStr/$localDateTime")
                val currentTime = LocalDateTime.now()
                val diffInMinutes = ChronoUnit.MINUTES.between(localDateTime, currentTime)
                if (diffInMinutes >= 15) {
                    Log.d(refreshTag, "Weather data timeout, refreshing for whole new data.")
                    fetchLocationAndWeather()
                }
                else {
                    Log.d(refreshTag, "Just only get new music recommendations (Weather data not timeout).")
                    findRelatedWeatherMusic(weatherData)
                }
            } catch (e: Exception) {
                Log.e(refreshTag, "Error parsing date-time string $dataTimeStr, just refreshing for whole new data.", e)
                fetchLocationAndWeather()
            }
        }
        else {
            fetchLocationAndWeather()
        }
    }

    private val _homeDataState = MutableStateFlow<HomeDataState>(HomeDataState.Initial)

    fun fetchLocationAndWeather() {
        if (_homeDataState.value is HomeDataState.Loading) return

        viewModelScope.launch {
            _homeDataState.value = HomeDataState.Loading

            val resultState = getLocationAndWeatherUseCase()
            _homeDataState.value = resultState


            when (resultState) {
                is HomeDataState.Success -> {
                    findRelatedWeatherMusic(resultState.data.weatherResponse)
                }

                HomeDataState.GpsDisabled -> {
                    _showGpsDialog.value = true
                }
                HomeDataState.MissingPermission -> {
                    Log.d("LocationTracker", "Missing location permission")
                }
                is HomeDataState.Error -> {
                    Log.d("LocationTracker", "Error getting location: ${resultState.message}")
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar("Error getting location. Please try again."))
                }
                else -> {}
            }

            _isRefreshing.value = false
        }
    }

    fun onGpsDialogDismiss() {
        _showGpsDialog.value = false
    }

    fun navigateToSettings() {
        uiEventManager.sendEvent(UiEvent.Navigate(SettingsScreen.Settings.route))
    }

    private var _findWeatherMusicUiState: MutableStateFlow<UiState<TwoTracksList>> =
        MutableStateFlow(UiState.Initial)
    val findWeatherMusicUiState: StateFlow<UiState<TwoTracksList>> = _findWeatherMusicUiState.asStateFlow()
    private var searchJob: Job? = null
    private val musicTag = "WeatherMusic"


    fun findRelatedWeatherMusic(weatherResponse: WeatherResponse) {

        // Check if the search is already in progress, if yes, then return and display initial.
        if (_findWeatherMusicUiState.value is UiState.Loading) {
            return
        }

        if (isMockMode) {
            viewModelScope.launch {
                _findWeatherMusicUiState.value = UiState.Loading
                delay(1800)
                _findWeatherMusicUiState.value = UiState.Success(
                    TwoTracksList(
                        MockData.mockSpotifyTracks.subList(0, 10),
                        MockData.mockSpotifyTracks.subList(10, 20)
                    )
                )
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarWithAction(
                        "Search successfully completed.",
                        MainScreen.Home.label
                    )
                )
                if (_isRefreshing.value) {
                    _isRefreshing.value = false
                }
            }
            return
        }

        searchJob = viewModelScope.launch {
            // Call UseCase and collect the results of the Flow
            findWeatherRelatedMusicUseCase(weatherResponse)
                .onStart {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar("You can explore other content in app, we'll inform you when it's ready!"))
                }
                .catch { exception ->
                    // If there's an uncaught exception within the Flow, handle it here
                    _findWeatherMusicUiState.value =
                        UiState.Error(exception.localizedMessage ?: "Flow collection error")
                }
                .collect { state ->
                    // Whenever the UseCase emits a new state, it will be received here
                    _findWeatherMusicUiState.value = state


                    // You can send UI events based on the state here
                    if (state is UiState.Success) {
                        uiEventManager.sendEvent(
                            UiEvent.ShowSnackbarWithAction(
                                "Search successfully completed.",
                                MainScreen.Home.label
                            )
                        )
                    } else if (state is UiState.Error) {
                        if (state.throwable is ServerException) {
                            uiEventManager.sendEvent(
                                UiEvent.ShowSnackbarDetail(
                                    "Gemini Server Error, please try again later.",
                                    state.throwable.stackTraceToString()
                                )
                            )
                        } else {
                            uiEventManager.sendEvent(
                                UiEvent.ShowSnackbarDetail(
                                    "An error occurred, please try again later.",
                                    state.message
                                )
                            )
                        }

                    }

                    if (state is UiState.Success || state is UiState.Error) {
                        if (_isRefreshing.value) {
                            _isRefreshing.value = false
                            Log.d(
                                musicTag,
                                "Refresh finished, isRefreshing set to false in finally."
                            )
                        }
                    }
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }



    fun fetchLatestRecommendation() {
        if (_recommendationUiState.value is RecommendationUiState.Success) return
        Log.d(tag, "Fetching latest recommendation")

        if (isMockMode) {
            viewModelScope.launch {
                delay(500)
                _recommendationUiState.value =
                    RecommendationUiState.Success(
                        WeeklyRecommendation(
                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            "For Your Everyday Recommendation",
                            null,
                            MockData.mockTrackFromCloudRecommendation
                        )
                    )
            }
        }

        viewModelScope.launch {
            _recommendationUiState.value = RecommendationUiState.Loading

            firebaseAuthRepository.getLatestRecommendation()
                .onSuccess { recommendation ->
                    if (recommendation != null) {
                        _recommendationUiState.value = RecommendationUiState.Success(recommendation)
                        Log.d(tag, "Latest recommendation fetched: $recommendation")
                    } else {
                        _recommendationUiState.value = RecommendationUiState.Empty
                        Log.d(tag, "No latest recommendation found")
                    }
                }
                .onFailure { e ->
                    _recommendationUiState.value = RecommendationUiState.Error(e.localizedMessage ?: "Unknown error")
                    Log.e(tag, "Error fetching latest recommendation", e)
                }
        }
    }


    private suspend fun checkIfEverydayRecommendationSeenAndTriggerModalBottomSheet() {
        val zoneId = java.time.ZoneId.systemDefault()
        val nowLocal = java.time.ZonedDateTime.now(zoneId)
        val nowUtc = java.time.ZonedDateTime.now(java.time.ZoneId.of("UTC"))

        val todayId = nowLocal.toLocalDate().toString()

        // 1. Check if today's recommendation is already seen.
        val lastViewedId = firebaseAuthRepository.lastUpdatedEverydayRecommendationDateFlow.first()
        if (lastViewedId == todayId) {
            Log.d(tag, "User has already seen today's recommendation ($todayId). Stop.")
            return
        }

        // 2. Modify the definition of server generation time
        // The cloud function is executed at UTC 20:00 (and save ID of "tomorrow")
        // Which means that the ID of "2025-12-30" (todayId) is generated at "2025-12-29" (Yesterday)
        val yesterdayId = nowLocal.minusDays(1).toLocalDate().toString()

        // To calculate "Today's data" should be ready at what time? Yesterday's UTC 20:00
        val serverGenerationTimeForToday = LocalDate.parse(todayId)
            .minusDays(1) // Push back for a day
            .atTime(20, 5) // UTC 20:00 + 5 mins buffer
            .atZone(java.time.ZoneId.of("UTC"))

        val isLocalTimeUnlocked = nowLocal.hour >= 8
        val isServerDataReady = nowUtc.isAfter(serverGenerationTimeForToday)

        // 3. Decide the target (Strategy)
        val targetDateId = if (isLocalTimeUnlocked && isServerDataReady) {
            todayId
        } else {
            // If today is locked or not ready, fallback to yesterday
            Log.d(tag, "Today is locked or not ready. Fallback to yesterday.")
            yesterdayId
        }

        // 4. Final comparison (Handle fallback situation)
        if (lastViewedId != targetDateId) {
            Log.i(tag, "New content found ($targetDateId)!")
            Log.d(tag, "Triggering fetching data from firestore and modal bottom sheet.")
            setBottomSheetVisibility(true)
            // Can use targetDateId to specifically fetch data from firestore for the date
            fetchLatestRecommendation()
        } else {
            Log.d(tag, "User has already seen the fallback target ($targetDateId).")
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
}