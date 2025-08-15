package com.example.geminispotifyapp.data

import com.google.gson.annotations.SerializedName

interface TrackInformation {
    val artists: List<SimplifiedArtist>
    val availableMarkets: List<String>
    val discNumber: Int
    val durationMs: Int
    val explicit: Boolean
    val externalUrls: Map<String, String>
    val href: String
    val id: String
    val isPlayable: Boolean
    val linkedFrom: Map<String, String>
    val restrictions: Map<String, String>
    val name: String
    val trackNumber: Int
    val type: String
    val uri: String
    val isLocal: Boolean
}

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

data class SimplifiedArtist(
    val id: String,
    val name: String,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val uri: String,
    val href: String
)

data class SpotifyTrack(
    val album: SpotifyAlbum, // Not provide in SimplifiedTrackObject
    override val artists: List<SimplifiedArtist>,
    @SerializedName("available_markets")
    override val availableMarkets: List<String>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val explicit: Boolean,
    @SerializedName("external_ids")
    val externalIds: Map<String, String>,// Not provide in SimplifiedTrackObject
    @SerializedName("external_urls")
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    @SerializedName("is_playable")
    override val isPlayable: Boolean,
    @SerializedName("linked_from")
    override val linkedFrom: Map<String, String>,
    override val restrictions: Map<String, String>,
    override val name: String,
    val popularity: Int,// Not provide in SimplifiedTrackObject
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val type: String,
    override val uri: String,
    @SerializedName("is_local")
    override val isLocal: Boolean,
): TrackInformation

data class SpotifyAlbum(
    val id: String,
    val name: String,
    val type: String,
    val images: List<SpotifyImage>,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val artists: List<SimplifiedArtist>,
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

data class Tracks(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val items: List<SpotifyTrack>
)

data class Albums(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val items: List<SpotifyAlbum>
)

data class Artists(
    val limit: Int,
    val offset: Int,
    val total: Int,
    val items: List<SpotifyArtist>
)


data class SearchResponse(
    val tracks: Tracks?,
    val artists: Artists?,
    val albums: Albums?
)

data class UserProfileResponse(
    val country: String,
    @SerializedName("display_name")
    val displayName: String,
    val email: String,
    @SerializedName("explicit_content")
    val explicitContent: Map<String, Boolean>,
    @SerializedName("external_urls")
    val externalUrls: Map<String, String>,
    val followers: Map<String, Int>,
    val id: String,
    val images: List<SpotifyImage>,
    val product: String,
    val type: String,
    val uri: String,
)

data class SimplifiedTrack(
    override val artists: List<SimplifiedArtist>,
    @SerializedName("available_markets")
    override val availableMarkets: List<String>,
    @SerializedName("disc_number")
    override val discNumber: Int,
    @SerializedName("duration_ms")
    override val durationMs: Int,
    override val explicit: Boolean,
    @SerializedName("external_urls")
    override val externalUrls: Map<String, String>,
    override val href: String,
    override val id: String,
    @SerializedName("is_playable")
    override val isPlayable: Boolean,
    @SerializedName("linked_from")
    override val linkedFrom: Map<String, String>,
    override val restrictions: Map<String, String>,
    override val name: String,
    @SerializedName("track_number")
    override val trackNumber: Int,
    override val type: String,
    override val uri: String,
    @SerializedName("is_local")
    override val isLocal: Boolean,
//    // Below values are not provided in actual SimplifiedTrackObject (but some use this data class structure for easy display)
//    val album: SpotifyAlbum?,
//    val popularity: Int?,
//    @SerializedName("external_ids")
//    val externalIds: Map<String, String>?,
): TrackInformation

data class SimplifiedTracksResponse(
    @SerializedName("items")
    val tracks: List<SimplifiedTrack>,
    val href: String,
    val limit: Int,
    val next: String?,
    val offset: Int,
    val previous: String?,
    val total: Int
)

data class TracksResponse(
    val tracks : List<SpotifyTrack>
)