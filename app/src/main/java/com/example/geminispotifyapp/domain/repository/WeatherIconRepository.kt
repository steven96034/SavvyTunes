package com.example.geminispotifyapp.domain.repository

interface WeatherIconRepository {
    fun getWeatherDisplayInfo(wmoCode: Int, isDay: Boolean): Pair<String?, String?>
}