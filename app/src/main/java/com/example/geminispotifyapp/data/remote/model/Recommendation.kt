package com.example.geminispotifyapp.data.remote.model

import com.google.firebase.Timestamp

data class WeeklyRecommendation(
    val id: String = "", // Firestore Document ID (YYYY-MM-DD)
    val summary: String = "",
    val generatedAt: Timestamp? = null,
    val tracks: List<TrackFromCloudRecommendation> = emptyList()
)

data class TrackFromCloudRecommendation(
    val spotifyId: String = "",
    val name: String = "",
    val artist: String = "",
    val imageUrl: String? = null,
    val uri: String = ""
)