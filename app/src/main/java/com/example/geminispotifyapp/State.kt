package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack

// For downloading process in SpotifyData
sealed interface DownLoadState {
    data object Initial : DownLoadState
    data object Loading : DownLoadState
    data class Success(val data: UserData) : DownLoadState
    data class Error(val data: ErrorData) : DownLoadState
}

data class UserData(
    val topArtistsShort: List<SpotifyArtist> = emptyList(),
    val topArtistsMedium: List<SpotifyArtist> = emptyList(),
    val topArtistsLong: List<SpotifyArtist> = emptyList(),
    val topTracksShort: List<SpotifyTrack> = emptyList(),
    val topTracksMedium: List<SpotifyTrack> = emptyList(),
    val topTracksLong: List<SpotifyTrack> = emptyList(),
    val recentlyPlayed: List<PlayHistoryObject> = emptyList()
)

data class ErrorData(
    val httpStatusCode: Int? = null,
    val errorMessage: String? = null,
    val errorCause: Throwable? = null
)

// For searching method in HomePage
sealed interface SearchUiState {
    data object Initial : SearchUiState
    data object Loading : SearchUiState
    data class Success(val data: TracksAndArtists) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class TracksAndArtists(
    val tracks: List<SpotifyTrack>?,
    val artists: List<SpotifyArtist>?
)