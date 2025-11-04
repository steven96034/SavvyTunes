package com.example.geminispotifyapp.presentation.features.main.home

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.data.remote.api.GeminiApi
import com.example.geminispotifyapp.data.repository.WeatherDataRepositoryImpl
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.domain.repository.WeatherIconRepository
import com.example.geminispotifyapp.domain.usecase.GetLocationAndWeatherUseCase
import com.example.geminispotifyapp.domain.usecase.SearchForSpecificTrackUseCase
import com.example.geminispotifyapp.presentation.MainScreen
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.ServerException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import javax.inject.Inject
import kotlin.collections.chunked
import kotlin.collections.map
import kotlin.text.split

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
    private val spotifyRepository: SpotifyRepository,
    private val uiEventManager: UiEventManager,
    private val searchForSpecificTrackUseCase: SearchForSpecificTrackUseCase,
    val weatherIconRepository: WeatherIconRepository,
    private val weatherDataRepositoryImpl: WeatherDataRepositoryImpl,
    private val getLocationAndWeatherUseCase: GetLocationAndWeatherUseCase,
    private val geminiApi: GeminiApi
): ViewModel() {
    private val _weatherDataJson = MutableStateFlow<WeatherResponse?>(null)
    val weatherDataJson: StateFlow<WeatherResponse?> = _weatherDataJson.asStateFlow()

    private val _showGpsDialog = MutableStateFlow(false)
    val showGpsDialog: StateFlow<Boolean> = _showGpsDialog

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        observeJsonWeatherData()
    }

    private fun observeJsonWeatherData() {
        viewModelScope.launch {
            weatherDataRepositoryImpl.weatherData.collect { weatherResponse ->
                _weatherDataJson.value = weatherResponse
                Log.d("HomeViewModel", "JSON Weather Data Updated: $weatherResponse")
            }
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

    private var _findWeatherMusicUiState: MutableStateFlow<UiState<TwoTracksList>> =
        MutableStateFlow(UiState.Initial)
    val findWeatherMusicUiState: StateFlow<UiState<TwoTracksList>> = _findWeatherMusicUiState.asStateFlow()
    private var searchJob: Job? = null
    private lateinit var responseRelated: GenerateContentResponse

    private val musicTag = "WeatherMusic"

    private var relatedTracksOfCondition = mutableListOf<String>()
    private var relatedTracksOfEmotion = mutableListOf<String>()

    // For Spotify API (Not-Found List is for debug.)
    private val conditionTempList = mutableListOf<SpotifyTrack>()
    private val conditionNotFoundList = mutableListOf<String>()

    private val emotionTempList = mutableListOf<SpotifyTrack>()
    private val emotionNotFoundList = mutableListOf<String>()


    fun findRelatedWeatherMusic(weatherResponse: WeatherResponse) {

        // Check if the search is already in progress, if yes, then return and display initial.
        if (_findWeatherMusicUiState.value is UiState.Loading) {
            return
        }
        val startTime = System.currentTimeMillis()
        var geminiFinishedTime = System.currentTimeMillis()

        searchJob = viewModelScope.launch {

            val currentJsonWeather = weatherResponse.current

            uiEventManager.sendEvent(UiEvent.ShowSnackbar("You can explore other content in app, we'll inform you when it's ready!"))
            _findWeatherMusicUiState.value = UiState.Loading

            try {
                val numOfShowCaseSearch = spotifyRepository.numOfShowCaseSearchFlow.first()
                val languageOfShowCaseSearch = spotifyRepository.languageOfShowCaseSearchFlow.first()
                val genreOfShowCaseSearch = spotifyRepository.genreOfShowCaseSearchFlow.first()
                val yearOfShowCaseSearch = spotifyRepository.yearOfShowCaseSearchFlow.first()
                val isRandomYearOfShowCaseSelection = spotifyRepository.isRandomYearOfShowCaseSelectionFlow.first()

                // For random year search in small range
                val randomNumber = (-5..5).random()
                var yearOfSearch = 0
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                if (yearOfShowCaseSearch != "") {
                    yearOfSearch = if (isRandomYearOfShowCaseSelection) {
                        val newYear = yearOfShowCaseSearch.toInt() + randomNumber
                        Log.d(musicTag, "yearOfSearch: $newYear (After added by random number: $randomNumber)")
                        if (newYear > currentYear) currentYear else newYear
                    } else {
                        yearOfShowCaseSearch.toInt()
                    }
                }

                Log.d(musicTag, "numOfShowCaseSearch: $numOfShowCaseSearch, languageOfShowCaseSearch: $languageOfShowCaseSearch, genreOfShowCaseSearch: $genreOfShowCaseSearch, yearOfShowCaseSearch: $yearOfShowCaseSearch")

                val numOfQuery = "$numOfShowCaseSearch"
                val languageOfQuery = if (languageOfShowCaseSearch != "") ", where the tracks should be written in $languageOfShowCaseSearch," else ", where the tracks should be written/included in a great variety of languages,"
                val genreOfQuery = if (genreOfShowCaseSearch != "") " (with genre $genreOfShowCaseSearch)" else ""
                val yearOfQuery = if (yearOfShowCaseSearch != "") " around A.D. $yearOfSearch" else ""

                val wmo = currentJsonWeather.weatherCode
                val temperature = currentJsonWeather.temperature.toFloat()
                val time = currentJsonWeather.time

                Log.d(musicTag, "Using JSON Weather Data -> wmo: $wmo, temperature: $temperature, time: $time")

                withContext(Dispatchers.IO) {
                    responseRelated = geminiApi.askGemini(
                        """Rules to respond: List only one related music of track$genreOfQuery$yearOfQuery in each row$languageOfQuery using format: Song Name##Album Name##Artists Name, while followed by its album and the artists,
                             if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track).
                             Use one blank row to separate the response of aforementioned weather condition and the response of related emotion of this weather.
                             Other response rule: Do not use No., and do not respond any other statement, neither.

                             Below is the main query:
                             The current weather represented in WMO weather interpretation code is $wmo, the current temperature is $temperature, and current time is $time.
                             Please recommend $numOfQuery related music tracks of aforementioned weather condition, where the format mentioned is: Song Name##Album Name##Artists Name.
                             Also, recommend $numOfQuery related music tracks of the related emotion of this weather and time, where the format mentioned is: Song Name##Album Name##Artists Name.
                             Notice: List only one related music track in each row using format: Song Name##Album Name##Artists Name
                                    """
                    )
                    Log.d(musicTag, "response: ${responseRelated.text}")
                    geminiFinishedTime = System.currentTimeMillis()
                    Log.d(
                        musicTag,
                        "Gemini response takes time: ${System.currentTimeMillis() - startTime}ms"
                    )
                    responseRelated.text?.trimIndent()?.let { outputContent ->
                        Log.d(musicTag, "trimmed response: $outputContent")
                        relatedTracksOfEmotion.clear()
                        relatedTracksOfCondition.clear()

                        val lines = outputContent.split("\n")
                        val blankLineIndex = lines.indexOf("")

                        if (blankLineIndex != -1) {
                            // First $numOfSearch rows: weather condition
                            relatedTracksOfCondition.addAll(
                                lines.subList(
                                    0,
                                    minOf(numOfShowCaseSearch, blankLineIndex)
                                )
                            )
                            // Last $numOfSearch rows: emotion and time
                            relatedTracksOfEmotion.addAll(
                                lines.subList(
                                    blankLineIndex + 1,
                                    minOf(blankLineIndex + numOfShowCaseSearch + 1, lines.size)
                                )
                            )
                        } else {
                            Log.e(musicTag, "Response format error")
                            // fallback to all tracks
                            relatedTracksOfCondition.addAll(lines.subList(0, minOf(numOfShowCaseSearch, lines.size)))
                        }
                        Log.d(musicTag, "relatedTracks: $relatedTracksOfCondition")
                        Log.d(musicTag, "relatedArtists: $relatedTracksOfEmotion")


                        conditionTempList.clear()
                        conditionNotFoundList.clear()
                        emotionTempList.clear()
                        emotionNotFoundList.clear()

                        val batchSize = 5
                        // Deal with the batch query of relatedTracks
                        relatedTracksOfCondition.chunked(batchSize).forEach { trackBatch ->
                            val deferredTrackResults = trackBatch.map { trackInfo ->
                                async { // Inherit Dispatchers.IO
                                    val parts = trackInfo.split("##")
                                    if (parts.size == 3) {
                                        val trackName = parts[0].trim()
                                        val albumName = parts[1].trim()
                                        val artistName = parts[2].trim()
                                        searchForSpecificTrackUseCase(trackName, albumName, artistName)
                                    } else {
                                        Log.e(musicTag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val trackResults = deferredTrackResults.awaitAll()
                            Log.d(musicTag, "Chunk search finished.")
                            trackResults.forEach { result ->
                                val (track, notFoundId) = result
                                if (track != null) {
                                    conditionTempList.add(track)
                                } else if (notFoundId != null) {
                                    conditionNotFoundList.add(notFoundId)
                                }
                            }
                        }

                        relatedTracksOfEmotion.chunked(batchSize).forEach { trackBatch ->
                            val deferredTrackResults = trackBatch.map { trackInfo ->
                                async { // Inherit Dispatchers.IO
                                    val parts = trackInfo.split("##")
                                    if (parts.size == 3) {
                                        val trackName = parts[0].trim()
                                        val albumName = parts[1].trim()
                                        val artistName = parts[2].trim()
                                        searchForSpecificTrackUseCase(trackName, albumName, artistName)
                                    } else {
                                        Log.e(musicTag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val trackResults = deferredTrackResults.awaitAll()
                            Log.d(musicTag, "Chunk search finished.")
                            trackResults.forEach { result ->
                                val (track, notFoundId) = result
                                if (track != null) {
                                    emotionTempList.add(track)
                                } else if (notFoundId != null) {
                                    emotionNotFoundList.add(notFoundId)
                                }
                            }
                        }

                        Log.d(musicTag, "conditionNotFoundList: $conditionNotFoundList")
                        Log.d(musicTag, "emotionNotFoundList: $emotionNotFoundList")
                        Log.d(musicTag, "conditionTempList: ${conditionTempList.joinToString { it.name }}")
                        Log.d(musicTag, "emotionTempList: ${emotionTempList.joinToString { it.name }}")

                        val data = TwoTracksList(
                            conditionTempList.toList(),
                            emotionTempList.toList(),
                        )
                        _findWeatherMusicUiState.value = UiState.Success(data)
                    //For test
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction("Search successfully completed.", MainScreen.Home.label))

                        Log.d(musicTag, "Tracks Data: $data")
                    }
                } ?: if (isActive) { // If response.text is null
                    _findWeatherMusicUiState.value =
                        UiState.Error("Failed to get a valid response from Gemini.")
                } else {
                    Log.d(musicTag, "Response is null.")
                }
            }
            catch (e: ServerException) {
                if (isActive) {
                    _findWeatherMusicUiState.value =
                        UiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.sendEvent(
                        UiEvent.ShowSnackbarDetail(
                            "Gemini Server Error, please try again later.", e.stackTraceToString()
                        )
                    )
                    Log.d(musicTag, "Error: $e")
                    e.printStackTrace()
                }
            }
            catch (e: Exception) {
                if (isActive) {
                    _findWeatherMusicUiState.value =
                        UiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.sendEvent(
                        UiEvent.ShowSnackbarDetail(
                            "Some error occurred when finding recommendations, please try again later.", e.stackTraceToString()
                        )
                    )
                    Log.d(musicTag, "Error: $e")
                    e.printStackTrace()
                }
            } finally {
                // The "isActive" here is the status of the coroutine that is in finally block
                // If the coroutine is cancelled, isActive will be false in finally block
                // Ensure that this is caused by cancellation and that searchJob is the one that is being cancelled.
                if (_findWeatherMusicUiState.value is UiState.Loading && !isActive && searchJob?.isCancelled == true) {
                    _findWeatherMusicUiState.value = UiState.Initial
                    Log.d(
                        musicTag,
                        "Search was cancelled and UI state reset to Initial in finally."
                    )
                }
                if (_isRefreshing.value) {
                    _isRefreshing.value = false
                    Log.d(musicTag, "Refresh finished, isRefreshing set to false in finally.")
                }
            }
            Log.d(musicTag, "Spotify API takes time: ${System.currentTimeMillis() - geminiFinishedTime}ms")
            Log.d(
                musicTag,
                "Search job finished. Overall time: ${(System.currentTimeMillis() - startTime)}ms"
            )
        }
    }
}