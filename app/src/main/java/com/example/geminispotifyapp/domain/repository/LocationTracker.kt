package com.example.geminispotifyapp.domain.repository

import com.example.geminispotifyapp.data.repository.LocationResult

interface LocationTracker {
    suspend fun getCurrentLocation(): LocationResult
}