package com.example.geminispotifyapp.data.repository

import android.util.Log
import com.example.geminispotifyapp.core.di.ApplicationScope
import com.example.geminispotifyapp.data.local.room.AppLocalDataSource
import com.example.geminispotifyapp.domain.repository.WeatherDataRepository
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
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
    private val weatherApiService: WeatherApiService,
    private val localDataSource: AppLocalDataSource,
    @ApplicationScope private val applicationScope: CoroutineScope
): WeatherDataRepository {
    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    override val weatherData: StateFlow<WeatherResponse?> = _weatherData.asStateFlow()

    init {
        // 在 Repository 初始化時，啟動一個長壽的協程
        applicationScope.launch {
            // 從本地資料庫讀取一次初始值
            // firstOrNull() 會取得 Flow 發射的第一個值，然後取消收集，非常高效
            val initialData = localDataSource.getSavedWeather().firstOrNull()
            if (initialData != null) {
                _weatherData.value = initialData
                Log.d("WeatherDataRepository", "Initial weather data loaded from local database.")
            } else {
                Log.d("WeatherDataRepository", "No initial weather data found in local database.")
            }
        }
    }

    /**
     * Fetches weather data from the API and updates the StateFlow.
     */
    override suspend fun fetchWeatherData(latitude: Double, longitude: Double): Result<WeatherResponse> {
        return try {
            Log.d("WeatherDataRepository", "Fetching weather data for latitude: $latitude, longitude: $longitude")
            val response = weatherApiService.getWeatherData(latitude, longitude)
            val formattedTime = response.current.time.replace("T", "  ")
            val finalResponse = response.copy(current = response.current.copy(time = formattedTime))
            _weatherData.value = finalResponse
            Log.d("WeatherDataRepository", "Weather data: time: $formattedTime, weatherCode: ${response.current.weatherCode}, temperature: ${response.current.temperature}, isDay: ${response.current.isDay}")
            Result.success(finalResponse)
        } catch (e: Exception) {
            Log.e("WeatherDataRepository", "Error fetching weather data: ${e.message}")
            Result.failure(e)        }
    }
}