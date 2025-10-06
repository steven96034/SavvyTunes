package com.example.geminispotifyapp.features.findmusic

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyDataList
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.features.UiEventManager
import com.example.geminispotifyapp.features.findmusic.domain.LocationResult
import com.example.geminispotifyapp.features.findmusic.domain.LocationTracker
import com.example.geminispotifyapp.features.home.GeminiApi
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
    private val uiEventManager: UiEventManager
): ViewModel() {
    private val client = OkHttpClient()
    private val TAG = "WeatherService"

    private val _wmo = MutableStateFlow<List<Float?>?>(null)
    val wmo: StateFlow<List<Float?>?> = _wmo

    private val _temperature2m = MutableStateFlow<List<Float?>?>(null)
    val temperature2m: StateFlow<List<Float?>?> = _temperature2m

    private val _allForecastTimes = MutableStateFlow<List<String?>>(emptyList())
    val allForecastTimes: StateFlow<List<String?>> = _allForecastTimes

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
                }
                is LocationResult.GpsDisabled -> {
                    _showGpsDialog.value = true
                }
                is LocationResult.MissingPermission -> {
                    Log.d("LocationTracker", "Missing location permission")
                }
                is LocationResult.Error -> {
                    // 處理其他錯誤，例如顯示一個 Snackbar
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
//        viewModelScope.launch {
//            fetchWeatherData()
//        }
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
}