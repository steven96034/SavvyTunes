package com.example.geminispotifyapp.domain.repository

import com.example.geminispotifyapp.data.repository.WeatherResponse
import kotlinx.coroutines.flow.StateFlow

interface WeatherDataRepository {
    val weatherData: StateFlow<WeatherResponse?>
    suspend fun fetchWeatherData(latitude: Double, longitude: Double): Result<WeatherResponse>
}