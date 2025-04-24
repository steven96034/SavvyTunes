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
    val followers: Map<String, Int>,
    val genres: List<String>,
    val images: List<SpotifyImage>?
)

data class SpotifyTrack(
    val id: String,
    val name: String,
    val popularity: Int,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val album: SpotifyAlbum,
    val artists: List<SpotifyArtist>,
    @SerializedName("duration_ms")
    val durationMs: Int,
    val uri: String,
    @SerializedName("preview_url")
    val previewUrl: String?,
    @SerializedName("available_markets")
    val availableMarkets: List<String>,
    @SerializedName("disc_number")
    val discNumber: Int,
    val explicit: Boolean,
    @SerializedName("track_number")
    val trackNumber: Int,
    @SerializedName("is_local")
    val isLocal: Boolean,
    @SerializedName("external_ids")
    val externalIds: Map<String, String>,
    val restrictions: Map<String, String>,
    @SerializedName("is_playable")
    val isPlayable: Boolean,

)

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val type: String,
    val images: List<SpotifyImage>,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val artists: List<SpotifyArtist>,
    @SerializedName("release_date")
    val releaseDate: String,
    @SerializedName("release_date_precision")
    val releaseDatePrecision: String,
    @SerializedName("total_tracks")
    val totalTracks: Int,
    val uri: String,
    @SerializedName("available_markets")
    val availableMarkets: List<String>
)

data class SpotifyImage(
    val url: String,
    val height: Int?,
    val width: Int?
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
