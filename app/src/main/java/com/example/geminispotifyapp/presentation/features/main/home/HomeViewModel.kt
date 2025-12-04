package com.example.geminispotifyapp.presentation.features.main.home

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.data.worker.SpotifyMetadataWorker
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.domain.repository.WeatherDataRepository
import com.example.geminispotifyapp.domain.repository.WeatherIconRepository
import com.example.geminispotifyapp.domain.usecase.FindWeatherRelatedMusic
import com.example.geminispotifyapp.domain.usecase.GetLocationAndWeatherUseCase
import com.example.geminispotifyapp.presentation.SettingsScreen
import com.google.ai.client.generativeai.type.ServerException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
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

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val uiEventManager: UiEventManager,
    private val findWeatherRelatedMusicUseCase: FindWeatherRelatedMusic,
    private val spotifyRepository: SpotifyRepository,
    val weatherIconRepository: WeatherIconRepository,
    private val weatherDataRepository: WeatherDataRepository,
    private val getLocationAndWeatherUseCase: GetLocationAndWeatherUseCase,
    @ApplicationContext private val context: Context
): ViewModel() {
    private val _weatherDataJson = MutableStateFlow<WeatherResponse?>(null)
    val weatherDataJson: StateFlow<WeatherResponse?> = _weatherDataJson.asStateFlow()

    private val _showGpsDialog = MutableStateFlow(false)
    val showGpsDialog: StateFlow<Boolean> = _showGpsDialog

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing


    init {
        if (true) {
            Log.d("HomeViewModel", "Before scheduling one-time metadata fetch")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<SpotifyMetadataWorker>() // Use OneTimeWorkRequestBuilder
                .setInitialDelay(15, TimeUnit.SECONDS) // Add 20 seconds initial delay
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    // The backoff delay for OneTimeWorkRequest can usually be shorter, e.g., 10 seconds
                    10,
                    TimeUnit.SECONDS
                )
                .build()
            Log.d("HomeViewModel", "Scheduling one-time metadata fetch")
            // Use enqueueUniqueWork instead of enqueueUniquePeriodicWork
            WorkManager.getInstance(context).enqueueUniqueWork(
                "SpotifyMetadataFetch", // Unique name
                ExistingWorkPolicy.KEEP, // Replace the old task every time to ensure the new one is always executed
                workRequest
            )
            Log.d("HomeViewModel", "Scheduled one-time metadata fetch")
        }
        observeJsonWeatherData()
    }

    private fun observeJsonWeatherData() {
        viewModelScope.launch {
            weatherDataRepository.weatherData.collect { weatherResponse ->
                _weatherDataJson.value = weatherResponse
                Log.d("HomeViewModel", "JSON Weather Data Updated: $weatherResponse")
            }
        }
    }

    private fun getWorkerFlag(): Boolean {
        var workerFlag: Boolean? = null
        viewModelScope.launch {
            workerFlag = spotifyRepository.workerFlagFlow.firstOrNull()
        }
        Log.d("HomeViewModel", "Worker Flag: $workerFlag")
        return workerFlag == true
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

    // Used for rapid loading.
    fun initAndStartFetchData() {
        viewModelScope.launch {
            if (_weatherDataJson.value != null) {
                val track = spotifyRepository.getRecommendedTracks()!! // "!!" For Test!, nullable test later
                _findWeatherMusicUiState =
                    MutableStateFlow(UiState.Success(track))
            } else {
                fetchLocationAndWeather()
            }
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
//    private lateinit var responseRelated: GenerateContentResponse
    private val musicTag = "WeatherMusic"
//
//    private var relatedTracksOfCondition = mutableListOf<String>()
//    private var relatedTracksOfEmotion = mutableListOf<String>()
//
//    // For Spotify API (Not-Found List is for debug.)
//    private val conditionTempList = mutableListOf<SpotifyTrack>()
//    private val conditionNotFoundList = mutableListOf<String>()
//
//    private val emotionTempList = mutableListOf<SpotifyTrack>()
//    private val emotionNotFoundList = mutableListOf<String>()

    fun findRelatedWeatherMusic(weatherResponse: WeatherResponse) {

//        searchJob?.cancel()

        // Check if the search is already in progress, if yes, then return and display initial.
        if (_findWeatherMusicUiState.value is UiState.Loading) {
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
                                "View Results"
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

    fun cancelSearch() {
        searchJob?.cancel()
        _findWeatherMusicUiState.value = UiState.Initial
    }

    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}