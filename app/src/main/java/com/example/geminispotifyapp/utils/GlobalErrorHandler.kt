package com.example.geminispotifyapp.utils

import android.util.Log
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.features.UiEvent
import javax.inject.Inject

class GlobalErrorHandler @Inject constructor(
    private val spotifyRepository: SpotifyRepository
) {
    suspend fun processError(error: ApiError, tag: String): UiEvent {
        return when (error) {
            is ApiError.BadRequest -> {
                Log.d(tag, "BadRequest: ${error.message}")
                UiEvent.ShowSnackbarDetail("Request format error, please check your input", "BadRequest: ${error.message}")
            }
            is ApiError.Forbidden -> {
                Log.d(tag, "Forbidden: ${error.message}")
                UiEvent.ShowSnackbarDetail("You don't have enough permission to perform this action, please check if you are a Premium user.", "Forbidden: ${error.message}")
            }
            is ApiError.HttpError -> {
                Log.d(tag, "HttpError: ${error.message}")
                UiEvent.ShowSnackbarDetail("Http error, please try again later.", "HttpError: ${error.message}")
            }
            is ApiError.NetworkConnectionError -> {
                Log.d(tag, "NetworkConnectionError: ${error.message}")
                UiEvent.ShowSnackbarDetail("Network connection error, please check your internet connection.", "NetworkConnectionError: ${error.message}")
            }
            is ApiError.NotFound -> {
                Log.d(tag, "NotFound: ${error.message}")
                UiEvent.ShowSnackbarDetail("Resource not found, please try again later.", "NotFound: ${error.message}")
            }
            is ApiError.ServerError -> {
                Log.d(tag, "ServerError: ${error.message}")
                UiEvent.ShowSnackbarDetail("Server error, please try again later.", "ServerError: ${error.message}")
            }
            is ApiError.TooManyRequests -> {
                Log.d(tag, "TooManyRequests: ${error.message}")
                UiEvent.ShowSnackbarDetail("Too many requests, please wait ${error.retryAfter ?: 5} seconds.", "TooManyRequests: ${error.message}")
            }
            is ApiError.Unauthorized -> {
                Log.d(tag, "Unauthorized: ${error.message}")
                spotifyRepository.performLogOutAndCleanUp()
                UiEvent.Unauthorized("Unauthorized, please log in again.")
            }
            else -> {
                Log.d(tag, "UnknownError of ApiError: ${error.message}")
                UiEvent.ShowSnackbarDetail("Unknown error, please try again later.", "UnknownError of ApiError: ${error.message}")
            }
        }
    }
}