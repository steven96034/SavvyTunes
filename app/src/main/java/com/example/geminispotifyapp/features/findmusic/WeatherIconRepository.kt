package com.example.geminispotifyapp.features.findmusic

import android.util.Log
import com.example.geminispotifyapp.di.WeatherInfoGist
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

// URL for the weather conditions JSON file provided by Gist
const val WMO_CONDITIONS_GIST_URL =
    "https://gist.githubusercontent.com/stellasphere/9490c195ed2b53c707087c8c2db4ec0c/raw/76b0cb0ef0bfd8a2ec988aa54e30ecd1b483495d/descriptions.json"


@Singleton
class WeatherIconRepository @Inject constructor(
    @WeatherInfoGist private val httpClient: OkHttpClient, // Inject OkHttpClient
    private val gson: Gson, // Inject Gson
) {
    private val tag = "WeatherIconRepo"
    // Use StateFlow to store the parsed weather icon map, emitted when the data is loaded
    private val _weatherIconMap = MutableStateFlow<Map<Int, WeatherIconInfo>?>(null)

    init {
        // Start a coroutine to fetch weather icon data from the network when the Repository is instantiated
        CoroutineScope(Dispatchers.IO).launch {
            fetchWeatherIcons()
        }
    }

    private fun fetchWeatherIcons() {
        try {
            val request = Request.Builder().url(WMO_CONDITIONS_GIST_URL).build()
            val response = httpClient.newCall(request).execute()

            if (response.isSuccessful) {
                val jsonString = response.body?.string()
                // Use TypeToken to handle the generic type (Map<Int, WeatherIconInfo>)
                val type = object : TypeToken<Map<Int, WeatherIconInfo>>() {}.type
                val map: Map<Int, WeatherIconInfo> = gson.fromJson(jsonString, type)
                _weatherIconMap.value = map // Update the value of StateFlow
                Log.d(
                    tag,
                    "Weather icons fetched successfully. Map size: ${map.size}"
                )
            } else {
                Log.e(
                    tag,
                    "Failed to fetch weather icons: ${response.code} - ${response.message}"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error fetching weather icons", e)
        }
    }


    /**
     * Gets the corresponding weather icon URL and description based on the WMO Code and day/night state.
     *
     * @param wmoCode WMO weather code.
     * @param isDay Indicates whether it is currently daytime.
     * @return A Pair<String?, String?>, where the first element is the full URL of the icon and the second is the description.
     *         Returns Pair(null, null) if the corresponding WMO Code is not found.
     */
    fun getWeatherDisplayInfo(wmoCode: Int, isDay: Boolean): Pair<String?, String?> {
        val iconInfo = _weatherIconMap.value?.get(wmoCode)

        return if (iconInfo != null) {
            val details = if (isDay) iconInfo.day else iconInfo.night
            Pair(details.imageUrl, details.description)
        } else {
            Pair(null, null) // Return null if the WMO Code is not found
        }
    }
}