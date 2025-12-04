package com.example.geminispotifyapp.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters


//data class TwoTracksList(
//    val tracksA: List<SpotifyTrack>?,
//    val tracksB: List<SpotifyTrack>?
//)
//
//data class SpotifyTrack(
//    val album: SpotifyAlbum, // Not provide in SimplifiedTrackObject
//    override val artists: List<SimplifiedArtist>,
//    @SerializedName("available_markets")
//    override val availableMarkets: List<String>?,
//    @SerializedName("disc_number")
//    override val discNumber: Int,
//    @SerializedName("duration_ms")
//    override val durationMs: Int,
//    override val explicit: Boolean,
//    @SerializedName("external_ids")
//    val externalIds: Map<String, String>,// Not provide in SimplifiedTrackObject
//    @SerializedName("external_urls")
//    override val externalUrls: Map<String, String>,
//    override val href: String,
//    override val id: String,
//    @SerializedName("is_playable")
//    override val isPlayable: Boolean?,
//    @SerializedName("linked_from")
//    override val linkedFrom: Map<String, String>,
//    override val restrictions: Map<String, String>,
//    override val name: String,
//    val popularity: Int,// Not provide in SimplifiedTrackObject
//    @SerializedName("track_number")
//    override val trackNumber: Int,
//    override val type: String,
//    override val uri: String,
//    @SerializedName("is_local")
//    override val isLocal: Boolean,
//): TrackInformation
//
//
//data class WeatherResponse(
//    @SerializedName("current") val current: CurrentData
//)
//
//data class CurrentData(
//    @SerializedName("time") val time: String,
//    @SerializedName("temperature_2m") val temperature: Double,
//    @SerializedName("weather_code") val weatherCode: Int,
//    @SerializedName("is_day") val isDay: Int
//)

@Database(
    entities = [TrackEntity::class, WeatherEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomTypeConverters::class) // 註冊我們的轉換器
abstract class StructuredDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun weatherDao(): WeatherDao
}