package com.example.geminispotifyapp.data

import com.google.gson.annotations.SerializedName

// Data class for API response
data class SpotifyArtist(
    val id: String,
    val name: String,
    val popularity: Int,
    val type: String,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val images: List<SpotifyImage>?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val popularity: Int,
    val type: String,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val album: SpotifyAlbum,
    val artists: List<SpotifyArtist>
)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val type: String,
    val images: List<SpotifyImage>
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
)

data class SpotifyPagingObject<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    @SerializedName("next")
    val nextUrl: String?,
    @SerializedName("previous")
    val previousUrl: String?
)

data class TopArtistsResponse(
    val items: List<SpotifyArtist>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class TopTracksResponse(
    val items: List<SpotifyTrack>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

data class PlayHistoryObject(
    val track: SpotifyTrack,
    @SerializedName("played_at")
    val playedAt: String,
    val context: PlayContext?
)

data class PlayContext(
    val type: String,
    val uri: String,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>?
)

data class RecentlyPlayedResponse(
    val items: List<PlayHistoryObject>,
    val limit: Int,
    @SerializedName("next")
    val nextUrl: String?,
    @SerializedName("cursors")
    val cursors: Map<String, String>?
)
