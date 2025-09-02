package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.SpotifyAlbum
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.TrackInformation
import com.example.geminispotifyapp.data.UserProfileResponse

// For downloading process in SpotifyData
sealed interface DownLoadState {
    data object Initial : DownLoadState
    data object Loading : DownLoadState
    data class Success(val data: UserProfileResponse) : DownLoadState
    data class Error(val message: String) : DownLoadState
}

// For searching method in HomePage
sealed interface SearchUiState {
    data object Initial : SearchUiState
    data object Loading : SearchUiState
    data class Success(val data: SpotifyDataList) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SpotifyDataList(
    val tracks: List<SpotifyTrack>?,
    val artists: List<SpotifyArtist>?,
    val albums: List<SpotifyAlbum>?,
    val trackInformation: List<TrackInformation>?
)