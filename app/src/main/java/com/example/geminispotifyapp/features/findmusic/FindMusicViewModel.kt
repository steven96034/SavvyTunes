package com.example.geminispotifyapp.features.findmusic

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.UiState
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.TwoTracksList
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.features.UiEventManager
import com.example.geminispotifyapp.features.findmusic.domain.LocationResult
import com.example.geminispotifyapp.features.findmusic.domain.LocationTracker
import com.example.geminispotifyapp.features.domain.GeminiApi
import com.example.geminispotifyapp.features.domain.SearchForSpecificTrackUseCase
import com.example.geminispotifyapp.init.MainScreen
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.ServerException
import com.openmeteo.sdk.Variable
import com.openmeteo.sdk.VariableWithValues
import com.openmeteo.sdk.VariablesSearch
import com.openmeteo.sdk.VariablesWithTime
import com.openmeteo.sdk.WeatherApiResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.chunked
import kotlin.collections.map
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs
import kotlin.text.split

@HiltViewModel
class FindMusicViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val locationTracker: LocationTracker,
    private val uiEventManager: UiEventManager,
    private val searchForSpecificTrackUseCase: SearchForSpecificTrackUseCase,
    val weatherIconRepository: WeatherIconRepository
): ViewModel() {
    private val client = OkHttpClient()
    private val TAG = "WeatherService"

    private val _wmo = MutableStateFlow<List<Float?>?>(null)

    private val _temperature2m = MutableStateFlow<List<Float?>?>(null)

    private val _allForecastTimes = MutableStateFlow<List<String?>>(emptyList())
    val allForecastTimes: StateFlow<List<String?>> = _allForecastTimes

    // Data display in UI (temp, wmo code, time)
    private val _currentWeatherData = MutableStateFlow<CurrentWeatherDisplayData?>(null)
    val currentWeatherData: StateFlow<CurrentWeatherDisplayData?> = _currentWeatherData.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location

    private val _showGpsDialog = MutableStateFlow(false)
    val showGpsDialog: StateFlow<Boolean> = _showGpsDialog

    fun fetchLocation() {
        viewModelScope.launch {
            when (val result = locationTracker.getCurrentLocation()) {
                is LocationResult.Success -> {
                    _location.value = result.location
                    fetchWeatherData(result.location.latitude, result.location.longitude)
                    findRelatedWeatherMusic()
                }
                is LocationResult.GpsDisabled -> {
                    _showGpsDialog.value = true
                }
                is LocationResult.MissingPermission -> {
                    Log.d("LocationTracker", "Missing location permission")
                }
                is LocationResult.Error -> {
                    Log.d("LocationTracker", "Error getting location")
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar("Error getting location. Please try again."))
                }
            }
        }
    }

    fun onGpsDialogDismiss() {
        _showGpsDialog.value = false
    }


    init {
        viewModelScope.launch {
            fetchLocation()
        }
    }

    /**
     * Helper function to convert a Unix Epoch timestamp (in seconds) to a human-readable date-time string.
     * @param unixTimestampSeconds The Unix Epoch timestamp in seconds.
     * @param pattern The date-time format string, defaulting to "yyyy-MM-dd HH:mm:ss".
     * @return The formatted date-time string, or "N/A" if the input is null, or the original string if conversion fails.
     */
    private fun formatUnixTimestampToDateTimeString(
        unixTimestampSeconds: Long?,
        pattern: String = "yyyy-MM-dd HH:mm:ss"
    ): String {
        if (unixTimestampSeconds == null) return "N/A"
        return try {
            val date = Date(unixTimestampSeconds * 1000L) // Convert seconds to milliseconds
            val sdf = SimpleDateFormat(pattern, Locale.getDefault()) // Use the default locale
            sdf.format(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp $unixTimestampSeconds", e)
            unixTimestampSeconds.toString() // Return the original string on conversion failure
        }
    }

    suspend fun fetchWeatherData(latitude: Double, longitude: Double) = withContext(Dispatchers.IO) {
        // Step 1 : Request
        // Found that "current" data is not transferred from OpenMeteo endpoint in flatbuffers format. (Maybe this data is not in demand of extremely effective transfer./)
        // So we just get the time near the current time in hourly data. (&current=temperature_2m,weather_code)
        val mUrl =
            "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&hourly=temperature_2m,weather_code&timezone=auto&forecast_days=1&format=flatbuffers"

        val request = Request.Builder()
            .url(mUrl)
            .get()
            .build()

        val responseBytes = suspendCoroutine { continuation ->
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Weather API request failed", e)
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            response.body?.bytes()?.let { bytes ->
                                continuation.resume(bytes)
                            } ?: run {
                                val error = IOException("Response body is null")
                                Log.e(TAG, "Weather API response body null", error)
                                continuation.resumeWithException(error)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to read response bytes", e)
                            continuation.resumeWithException(e)
                        } finally {
                            response.close()
                        }
                    } else {
                        val error = IOException("Unexpected response code: ${response.code}")
                        Log.e(
                            TAG,
                            "Weather API request not successful: ${response.code}",
                            error
                        )
                        continuation.resumeWithException(error)
                    }
                }
            })
        }

        responseBytes.let { bytes ->
            try {
                // Step 2 : Use Binary Response buffer and convert it to ByteBuffer
                val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

                // Step 3 : create the ApiResponse Instance
                val mApiResponse =
                    WeatherApiResponse.getRootAsWeatherApiResponse(buffer.position(4) as ByteBuffer)

                val hourly: VariablesWithTime? = mApiResponse.hourly()

                val timeValues = mutableListOf<String?>()
                val tempValues = mutableListOf<Float?>()
                val wmoValues = mutableListOf<Float?>()

                hourly?.let {
                    val temperature2m: VariableWithValues? = VariablesSearch(it)
                        .variable(Variable.temperature)
                        .altitude(2)
                        .first()
                    val wmo: VariableWithValues? = VariablesSearch(it)
                        .variable(Variable.weather_code)
                        .first()
                    val hourlyBlockStartTime = it.time()
                    val hourlyIntervalSeconds = it.interval()

                    withContext(Dispatchers.Default) {
                        if (temperature2m != null && wmo != null) {
                            for (vl in 0 until temperature2m.valuesLength()) {
                                val forecastTime =
                                    hourlyBlockStartTime + (vl * hourlyIntervalSeconds)
                                val temp = temperature2m.values(vl)
                                val wmoCode = wmo.values(vl)
                                val forecastTimeString =
                                    formatUnixTimestampToDateTimeString(forecastTime)
                                Log.d(
                                    TAG,
                                    "Hourly forecast at $forecastTimeString: Temperature -> $temp / WMO -> $wmoCode"
                                )

                                timeValues.add(forecastTimeString)
                                tempValues.add(temp)
                                wmoValues.add(wmoCode)
                            }

                        } else {
                            Log.w(TAG, "Hourly temperature or WMO data not found.")
                        }
                    }
                } ?: Log.w(TAG, "Hourly data is null.")
                _allForecastTimes.value = timeValues.toList()
                _temperature2m.value = tempValues.toList()
                _wmo.value = wmoValues.toList()

                buffer.clear()
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing FlatBuffer response", e)
                null
            }
        }
    }
    private var _findWeatherMusicUiState: MutableStateFlow<UiState<TwoTracksList>> =
        MutableStateFlow(UiState.Initial)
    val findWeatherMusicUiState: StateFlow<UiState<TwoTracksList>> = _findWeatherMusicUiState.asStateFlow()
    private var searchJob: Job? = null
    private lateinit var responseRelated: GenerateContentResponse

    private val tag = "FindMusicViewModel"

    private var relatedTracksOfCondition = mutableListOf<String>()
    private var relatedTracksOfEmotion = mutableListOf<String>()

    // For Spotify API (Not-Found List is for debug.)
    private val conditionTempList = mutableListOf<SpotifyTrack>()
    private val conditionNotFoundList = mutableListOf<String>()

    private val emotionTempList = mutableListOf<SpotifyTrack>()
    private val emotionNotFoundList = mutableListOf<String>()


    fun findRelatedWeatherMusic() {

        // Check if the search is already in progress, if yes, then return and display initial.
        if (_findWeatherMusicUiState.value is UiState.Loading) {
//            searchJob?.cancel(CancellationException("Cancel search by user."))
//            viewModelScope.launch {
//                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Previous search has successfully cancelled."))
//            }
//            _findWeatherMusicUiState.value = UiState.Initial
            return
        }
        val startTime = System.currentTimeMillis()
        var geminiFinishedTime = System.currentTimeMillis()

        searchJob = viewModelScope.launch {

            val nearestTimeIndex = findNearestForecastTime(allForecastTimes.value)
            if (nearestTimeIndex == null) {
                return@launch
            }
            Log.d(tag, "nearestTimeIndex: $nearestTimeIndex")

            uiEventManager.sendEvent(UiEvent.ShowSnackbar("You can explore other content in app, we'll inform you when it's ready!"))
            _findWeatherMusicUiState.value = UiState.Loading

            try {
                val numOfSearch = 15
                val language = "English"
                val genre = "Country"
                val year = "2010"
                val wmo = _wmo.value!![nearestTimeIndex]!!.toInt()
                val temperature = _temperature2m.value!![nearestTimeIndex]

                Log.d(tag, "nearestTimeIndex: $nearestTimeIndex, wmo: $wmo, temperature: $temperature")

                withContext(Dispatchers.IO) {
                    // Song name and album name for artists list is redundant for now, more precise for future.
                    responseRelated = GeminiApi().askGemini(
                        """Rules to respond: List only one related music of $language track around A.D. $year in each row using format: Song Name##Album Name##Artists Name, while followed by its album and the artists,
                             if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track).
                             Use one blank row to separate the response of aforementioned weather condition and the response of related emotion of this weather.

                             Below is the main query:
                             The current weather represented in WMO weather interpretation code is $wmo, the current temperature is $temperature.
                             Please recommend $numOfSearch related music tracks of aforementioned weather condition, where the format mentioned is: Song Name##Artists Name.
                             Also, recommend $numOfSearch related music tracks of the related emotion of this weather, where the format mentioned is: Song Name##Artists Name.
                             Notice: List only one related music track in each row using format: Song Name##Album Name##Artists Name
                                    """
                    )
                    Log.d(tag, "response: ${responseRelated.text}")
                    geminiFinishedTime = System.currentTimeMillis()
                    Log.d(
                        tag,
                        "Gemini response takes time: ${System.currentTimeMillis() - startTime}ms"
                    )
                    responseRelated.text?.trimIndent()?.let { outputContent ->
                        Log.d(tag, "trimmed response: $outputContent")
                        relatedTracksOfEmotion.clear()
                        relatedTracksOfCondition.clear()

                        val lines = outputContent.split("\n")
                        val blankLineIndex = lines.indexOf("")

                        if (blankLineIndex != -1) {
                            // First $numOfSearch rows: weather condition
                            relatedTracksOfCondition.addAll(
                                lines.subList(
                                    0,
                                    minOf(numOfSearch, blankLineIndex)
                                )
                            )
                            // Last $numOfSearch rows: emotion
                            relatedTracksOfEmotion.addAll(
                                lines.subList(
                                    blankLineIndex + 1,
                                    minOf(blankLineIndex + numOfSearch + 1, lines.size)
                                )
                            )
                        } else {
                            Log.e(tag, "Response format error")
                            // fallback to all tracks
                            relatedTracksOfCondition.addAll(lines.subList(0, minOf(numOfSearch, lines.size)))
                        }
                        Log.d(tag, "relatedTracks: $relatedTracksOfCondition")
                        Log.d(tag, "relatedArtists: $relatedTracksOfEmotion")


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
                                        Log.e(tag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val trackResults = deferredTrackResults.awaitAll()
                            Log.d(tag, "Chunk search finished.")
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
                                        Log.e(tag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val trackResults = deferredTrackResults.awaitAll()
                            Log.d(tag, "Chunk search finished.")
                            trackResults.forEach { result ->
                                val (track, notFoundId) = result
                                if (track != null) {
                                    emotionTempList.add(track)
                                } else if (notFoundId != null) {
                                    emotionNotFoundList.add(notFoundId)
                                }
                            }
                        }

                        Log.d(tag, "conditionNotFoundList: $conditionNotFoundList")
                        Log.d(tag, "emotionNotFoundList: $emotionNotFoundList")
                        Log.d(tag, "conditionTempList: ${conditionTempList.joinToString { it.name }}")
                        Log.d(tag, "emotionTempList: ${emotionTempList.joinToString { it.name }}")

                        val data = TwoTracksList(
                            conditionTempList.toList(),
                            emotionTempList.toList(),
                        )
                        _findWeatherMusicUiState.value = UiState.Success(data)
                    //For test
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction("Search successfully completed.", MainScreen.FindMusic.label))

                        Log.d(tag, "Tracks Data: $data")
                    }
                } ?: if (isActive) { // If response.text is null
                    _findWeatherMusicUiState.value =
                        UiState.Error("Failed to get a valid response from Gemini.")
                } else {
                    Log.d(tag, "Response is null.")
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
                    Log.d(tag, "Error: $e")
                    e.printStackTrace()
                }
            }
            catch (e: Exception) {
                if (isActive) {
                    _findWeatherMusicUiState.value =
                        UiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.sendEvent(
                        UiEvent.ShowSnackbarDetail(
                            "Some error occurred when searching similar tracks and artists, please try again later.", e.stackTraceToString()
                        )
                    )
                    Log.d(tag, "Error: $e")
                    e.printStackTrace()
                }
            } finally {
                // The "isActive" here is the status of the coroutine that is in finally block
                // If the coroutine is cancelled, isActive will be false in finally block
                // Ensure that this is caused by cancellation and that searchJob is the one that is being cancelled.
                if (_findWeatherMusicUiState.value is UiState.Loading && !isActive && searchJob?.isCancelled == true) {
                    _findWeatherMusicUiState.value = UiState.Initial
                    Log.d(
                        tag,
                        "Search was cancelled and UI state reset to Initial in finally."
                    )
                }
            }
            Log.d(tag, "Spotify API takes time: ${System.currentTimeMillis() - geminiFinishedTime}ms")
            Log.d(
                tag,
                "Search job finished. Overall time: ${(System.currentTimeMillis() - startTime)}ms"
            )
        }
    }

    private suspend fun findNearestForecastTime(forecastTimes: List<String?>): Int? = withContext(Dispatchers.Default) {
        if (forecastTimes.isEmpty()) {
            return@withContext null
        }

        val currentTimeMillis = System.currentTimeMillis()
        var minDiff = Long.MAX_VALUE
        var nearestTimeIndex: Int? = null

        forecastTimes.forEachIndexed { index, forecastTimeString ->
            val forecastTimeMillis = parseDateTimeStringToUnixTimestamp(forecastTimeString)
            if (forecastTimeMillis != null) {
                val diff = abs(currentTimeMillis - forecastTimeMillis)
                if (diff < minDiff) {
                    minDiff = diff
                    nearestTimeIndex = index
                }
            }
        }
        nearestTimeIndex?.let { index ->
            val temp = _temperature2m.value?.get(index)
            val wmoCode = _wmo.value?.get(index)
            val time = _allForecastTimes.value[index]
            val isDay = time?.let {
                val hour = it.substring(11, 13).toIntOrNull()
                hour != null && hour in 6..17
            } ?: true // Default to day if time is null
            if (temp != null && wmoCode != null && time != null) {
                _currentWeatherData.value = CurrentWeatherDisplayData(temp, wmoCode.toInt(), time, isDay)
            } else {
                Log.e(tag, "Cannot set current weather data due to null values.")
            }
        }
        nearestTimeIndex
    }

    private fun parseDateTimeStringToUnixTimestamp(
        dateTimeString: String?,
        pattern: String = "yyyy-MM-dd HH:mm:ss"
    ): Long? {
        if (dateTimeString == null || dateTimeString == "N/A") return null
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            sdf.parse(dateTimeString)?.time // Returns milliseconds since epoch
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date-time string $dateTimeString", e)
            null
        }
    }
}