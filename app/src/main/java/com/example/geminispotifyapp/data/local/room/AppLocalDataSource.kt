package com.example.geminispotifyapp.data.local.room

import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyImage
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.repository.CurrentData
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.presentation.features.main.home.TwoTracksList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocalDataSource @Inject constructor(
    private val trackDao: TrackDao,
    private val weatherDao: WeatherDao
) {
    // --- Track Related Methods ---
    private val json = Json { ignoreUnknownKeys = true }


    fun getSavedTracks(): Flow<TwoTracksList> {
        return trackDao.getAllTracks().map { trackEntities ->
            // 將 Entity 轉換回業務模型
            val tracksA = trackEntities.filter { it.listType == "CONDITION" }.map { it.toSpotifyTrack() }
            val tracksB = trackEntities.filter { it.listType == "EMOTION" }.map { it.toSpotifyTrack() }
            TwoTracksList(tracksA, tracksB)
        }
    }

    suspend fun saveTracks(twoTracksList: TwoTracksList) {
        val entitiesToSave = mutableListOf<TrackEntity>()
        twoTracksList.tracksA?.forEach { track ->
            entitiesToSave.add(track.toTrackEntity("CONDITION"))
        }
        twoTracksList.tracksB?.forEach { track ->
            entitiesToSave.add(track.toTrackEntity("EMOTION"))
        }
        trackDao.overwriteTracks(entitiesToSave)
    }

    // --- Weather Related Methods ---

    fun getSavedWeather(): Flow<WeatherResponse?> {
        return weatherDao.getWeather().map { weatherEntity ->
            // 將 Entity 轉換回業務模型
            weatherEntity?.toWeatherResponse()
        }
    }

    suspend fun saveWeather(weatherResponse: WeatherResponse) {
        // 將業務模型轉換為 Entity 並儲存
        val weatherEntity = weatherResponse.toWeatherEntity()
        weatherDao.upsert(weatherEntity)
    }

    // --- Mapper functions (可以放在伴生物件或擴展函式中) ---
// 將資料庫實體 TrackEntity 轉換回業務模型 SpotifyTrack
    private fun TrackEntity.toSpotifyTrack(): SpotifyTrack {
        return SpotifyTrack(
            // 直接映射的基礎類型
            id = this.id,
            name = this.name,
            popularity = this.popularity,
            durationMs = this.durationMs,
            explicit = this.explicit,
            discNumber = this.discNumber,
            trackNumber = this.trackNumber,
            type = this.type,
            uri = this.uri,
            isLocal = this.isLocal,
            href = this.href, // 假設 TrackEntity 中有 href 欄位
            isPlayable = this.isPlayable, // 假設 TrackEntity 中有 isPlayable 欄位

            // 透過 TypeConverter 處理的複雜類型
            artists = this.artists,
            availableMarkets = this.availableMarkets,
            externalUrls = this.externalUrls,
            externalIds = this.externalIds,
            linkedFrom = this.linkedFrom,
            restrictions = this.restrictions,

            // 從扁平化的 AlbumDataForDb 轉換回 SpotifyAlbum 物件
            album = this.album.toSpotifyAlbum()
        )
    }

    // 將業務模型 SpotifyTrack 轉換為資料庫實體 TrackEntity
    private fun SpotifyTrack.toTrackEntity(listType: String): TrackEntity {
        return TrackEntity(
            // 直接映射的基礎類型
            id = this.id,
            name = this.name,
            popularity = this.popularity,
            durationMs = this.durationMs,
            explicit = this.explicit,
            discNumber = this.discNumber,
            trackNumber = this.trackNumber,
            type = this.type,
            uri = this.uri,
            isLocal = this.isLocal,
            href = this.href,
            isPlayable = this.isPlayable ?: false,

            // 透過 TypeConverter 處理的複雜類型
            artists = this.artists,
            availableMarkets = this.availableMarkets,
            externalUrls = this.externalUrls,
            externalIds = this.externalIds,
            linkedFrom = this.linkedFrom,
            restrictions = this.restrictions,

            // 將 SpotifyAlbum 物件轉換為扁平化的 AlbumDataForDb
            album = this.album.toAlbumDataForDb(),

            // Entity 的額外欄位
            listType = listType
        )
    }

    // 將資料庫實體 WeatherEntity 轉換回業務模型 WeatherResponse
    private fun WeatherEntity.toWeatherResponse(): WeatherResponse {
        return WeatherResponse(
            current = CurrentData(
                time = this.time,
                temperature = this.temperature,
                weatherCode = this.weatherCode,
                isDay = this.isDay
            )
        )
    }

    // 將業務模型 WeatherResponse 轉換為資料庫實體 WeatherEntity
    private fun WeatherResponse.toWeatherEntity(): WeatherEntity {
        return WeatherEntity(
            // id 使用預設值 0
            time = this.current.time,
            temperature = this.current.temperature,
            weatherCode = this.current.weatherCode,
            isDay = this.current.isDay
        )
    }

    // 將 SpotifyAlbum 轉換為扁平化的 AlbumDataForDb
    private fun SpotifyAlbum.toAlbumDataForDb(): AlbumDataForDb {
        return AlbumDataForDb(
            album_id = this.id,
            album_name = this.name,
            album_type = this.type,
            album_releaseDate = this.releaseDate,
            album_totalTracks = this.totalTracks,
            album_uri = this.uri,

            // 進行序列化
            // ✅ Add explicit serializer for List<SpotifyImage>
            album_images = json.encodeToString(ListSerializer(SpotifyImage.serializer()), this.images),
            album_artists = json.encodeToString(ListSerializer(SimplifiedArtist.serializer()), this.artists), // Assuming SpotifyArtist is also serializable
            album_availableMarkets = json.encodeToString(ListSerializer(String.serializer()), this.availableMarkets), // 注意這裡的 availableMarkets 是 List<String> 而非 List<String>?

            // ✅ 新增的序列化邏輯
            album_externalUrls = json.encodeToString(
                MapSerializer(String.serializer(), String.serializer()), // <--- 明確提供 Map 的 Serializer
                this.externalUrls),
            album_releaseDatePrecision = this.releaseDatePrecision,
        )
    }

    // 將扁平化的 AlbumDataForDb 轉換回業務模型 SpotifyAlbum
    private fun AlbumDataForDb.toSpotifyAlbum(): SpotifyAlbum {
        return SpotifyAlbum(
            // --- 直接映射的欄位 ---
            id = this.album_id,
            name = this.album_name,
            type = this.album_type,
            releaseDate = this.album_releaseDate,
            releaseDatePrecision = this.album_releaseDatePrecision,
            totalTracks = this.album_totalTracks,
            uri = this.album_uri,

            // --- 需要反序列化的欄位 ---

            // 將 album_images (JSON String) 反序列化回 List<SpotifyImage>
            images = json.decodeFromString(
                ListSerializer(SpotifyImage.serializer()),
                this.album_images
            ),

            // 將 album_artists (JSON String) 反序列化回 List<SimplifiedArtist>
            artists = json.decodeFromString(
                ListSerializer(SimplifiedArtist.serializer()),
                this.album_artists
            ),

            // 將 album_availableMarkets (Nullable JSON String) 反序列化回 List<String>
            // 處理了來源可能為 null 的情況，如果為 null 則返回一個空列表
            availableMarkets = this.album_availableMarkets?.let { jsonString ->
                json.decodeFromString(ListSerializer(String.serializer()), jsonString)
            } ?: emptyList(),

            // 將 album_externalUrls (JSON String) 反序列化回 Map<String, String>
            externalUrls = json.decodeFromString(
                MapSerializer(String.serializer(), String.serializer()),
                this.album_externalUrls
            )
        )
    }
}