package com.example.geminispotifyapp.data.local.room

import androidx.room.TypeConverter
import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyImage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RoomTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }

    // For List<SimplifiedArtist>
    @TypeConverter
    fun fromArtistList(artists: List<SimplifiedArtist>): String {
        return json.encodeToString(artists)
    }

    @TypeConverter
    fun toArtistList(jsonString: String): List<SimplifiedArtist> {
        return json.decodeFromString(jsonString)
    }

    // For List<String>
    @TypeConverter
    fun fromStringList(strings: List<String>?): String? {
        return strings?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toStringList(jsonString: String?): List<String>? {
        return jsonString?.let { json.decodeFromString(it) }
    }

    // For Map<String, String>
    @TypeConverter
    fun fromStringMap(map: Map<String, String>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun toStringMap(jsonString: String): Map<String, String> {
        return json.decodeFromString(jsonString)
    }

    @TypeConverter
    fun fromImageList(images: List<SpotifyImage>): String {
        // ✅ 為了與其他轉換器一致，明確提供 Serializer
        return json.encodeToString(ListSerializer(SpotifyImage.serializer()), images)
    }

    // ✅ 新增配對的 toImageList 方法
    @TypeConverter
    fun toImageList(jsonString: String): List<SpotifyImage> {
        return json.decodeFromString(ListSerializer(SpotifyImage.serializer()), jsonString)
    }
}