package com.example.geminispotifyapp.data.repository

import android.util.Log
import com.example.geminispotifyapp.domain.repository.WeatherDataRepository
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton


// Data models to match the JSON structure
data class WeatherResponse(
    @SerializedName("current") val current: CurrentData
)

data class CurrentData(
    @SerializedName("time") val time: String,
    @SerializedName("temperature_2m") val temperature: Double,
    @SerializedName("weather_code") val weatherCode: Int,
    @SerializedName("is_day") val isDay: Int
)

sealed class WeatherResult{
    data class Success(val weatherResponse: WeatherResponse) : WeatherResult()
    data class Error(val exception: Exception) : WeatherResult()
}

// Retrofit service interface
interface WeatherApiService {
    // v1/forecast?latitude=24.212371880320966&longitude=120.70398004277607&hourly=temperature_2m,weather_code&timezone=auto&forecast_days=1
    @GET("v1/forecast?current=temperature_2m,weather_code,is_day&timezone=auto&forecast_days=1")
    suspend fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
    ): WeatherResponse
}

@Singleton
class WeatherDataRepositoryImpl @Inject constructor(
    private val weatherApiService: WeatherApiService
): WeatherDataRepository {
    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    override val weatherData: StateFlow<WeatherResponse?> = _weatherData.asStateFlow()

    /**
     * Fetches weather data from the API and updates the StateFlow.
     */
    override suspend fun fetchWeatherData(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            Log.d("WeatherDataRepository", "Fetching weather data for latitude: $latitude, longitude: $longitude")
            val response = weatherApiService.getWeatherData(latitude, longitude)
            val formattedTime = response.current.time.replace("T", "  ")
            _weatherData.value = response.copy(current = response.current.copy(time = formattedTime))
            Log.d("WeatherDataRepository", "Weather data: time: $formattedTime, weatherCode: ${response.current.weatherCode}, temperature: ${response.current.temperature}, isDay: ${response.current.isDay}")
            Result.success(response.copy(current = response.current.copy(time = formattedTime)))
        } catch (e: Exception) {
            Log.e("WeatherDataRepository", "Error fetching weather data: ${e.message}")
            Result.failure(e)        }
    }
}