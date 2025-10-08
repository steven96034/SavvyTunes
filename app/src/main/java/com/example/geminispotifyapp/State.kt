package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.SpotifyAlbum
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.TrackInformation
import com.example.geminispotifyapp.data.UserProfileResponse

sealed interface UiState<out T> {
    data object Initial : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
}
// For downloading process in SpotifyData
sealed interface DownLoadState {
    data object Initial : DownLoadState
    data object Loading : DownLoadState
    data class Success(val data: UserProfileResponse) : DownLoadState
    data class Error(val message: String) : DownLoadState
}

data class SpotifyDataList(
    val tracks: List<SpotifyTrack>?,
    val artists: List<SpotifyArtist>?,
    val albums: List<SpotifyAlbum>?,
    val trackInformation: List<TrackInformation>?
)

data class TwoTracksList(
    val tracksA: List<SpotifyTrack>?,
    val tracksB: List<SpotifyTrack>?
)