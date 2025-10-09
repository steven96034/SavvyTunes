package com.example.geminispotifyapp.features.findmusic

import com.google.gson.annotations.SerializedName

/**
 * Used to parse the weather information for each WMO Code in the Gist JSON.
 * Structure: "WMO_CODE": { "day": { ... }, "night": { ... } }
 */
data class WeatherDetails(
    @SerializedName("description") val description: String,
    @SerializedName("image") private val _imageUrl: String
) {
    val imageUrl: String
        get() = _imageUrl.replace("http://", "https://") // Replace with HTTPS here
}

/**
 * Used to parse the weather information for each WMO Code in the Gist JSON.
 * Structure: "WMO_CODE": { "day": { ... }, "night": { ... } }
 */
data class WeatherIconInfo(
    @SerializedName("day") val day: WeatherDetails,
    @SerializedName("night") val night: WeatherDetails
)

/**
 * Complete data for displaying weather, including temperature, weather code, time, and day/night status
 */
data class CurrentWeatherDisplayData(
    val temperature: Float,
    val weatherCode: Int,
    val time: String,
    val isDay: Boolean
)