package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack

// Data class for screen state
//data class ScreenStateTBD(
//    val isLoading: Boolean = true,
//    val errorMessage: String? = null,
//    val errorCause: Throwable? = null,
//    val topArtists: List<SpotifyArtist> = emptyList(),
//    val topTracks: List<SpotifyTrack> = emptyList(),
//    val recentlyPlayed: List<PlayHistoryObject> = emptyList()
//)

data class ScreenState(
    val isLoading: Boolean = true,
    val httpStatusCode: Int? = null,
    val errorMessage: String? = null,
    val errorCause: Throwable? = null,
    val topArtistsShort: List<SpotifyArtist> = emptyList(),
    val topArtistsMedium: List<SpotifyArtist> = emptyList(),
    val topArtistsLong: List<SpotifyArtist> = emptyList(),
    val topTracksShort: List<SpotifyTrack> = emptyList(),
    val topTracksMedium: List<SpotifyTrack> = emptyList(),
    val topTracksLong: List<SpotifyTrack> = emptyList(),
    val recentlyPlayed: List<PlayHistoryObject> = emptyList()
)