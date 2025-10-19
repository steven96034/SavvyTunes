package com.example.geminispotifyapp.data.remote.model

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