package com.example.geminispotifyapp.domain.usecase

import android.util.Log
import com.example.geminispotifyapp.data.repository.LocationResult
import com.example.geminispotifyapp.domain.repository.LocationTracker
import com.example.geminispotifyapp.domain.repository.WeatherDataRepository
import com.example.geminispotifyapp.presentation.features.main.home.HomeDataPayload
import com.example.geminispotifyapp.presentation.features.main.home.HomeDataState
import javax.inject.Inject

class GetLocationAndWeatherUseCase @Inject constructor(
    private val locationTracker: LocationTracker,
    private val weatherDataRepository: WeatherDataRepository
) {
    suspend operator fun invoke(): HomeDataState {
        return when (val locationResult = locationTracker.getCurrentLocation()) {
            is LocationResult.Success -> {
                val location = locationResult.location

                val time = System.currentTimeMillis()
                val weatherResult = weatherDataRepository.fetchWeatherData(
                    location.latitude,
                    location.longitude
                )
                Log.d("WeatherTest", "WeatherData fetch (Json from Retrofit) finished in ${System.currentTimeMillis() - time} ms")


                weatherResult.fold(
                    onSuccess = { weatherResponse ->
                        HomeDataState.Success(HomeDataPayload(location, weatherResponse))
                    },
                    onFailure = { exception ->
                        HomeDataState.Error("Failed to fetch weather data: ${exception.message}")
                    }
                )
            }

            is LocationResult.GpsDisabled -> HomeDataState.GpsDisabled
            is LocationResult.MissingPermission -> HomeDataState.MissingPermission
            is LocationResult.Error -> HomeDataState.Error("Error getting location: ${locationResult.exception?.message}")
        }
    }
}