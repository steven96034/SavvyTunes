package com.example.geminispotifyapp.data.local.room

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val popularity: Int,
    val durationMs: Int,
    val explicit: Boolean,
    val discNumber: Int,
    val trackNumber: Int,
    val type: String,
    val uri: String,
    val isLocal: Boolean,
    val href: String,
    val isPlayable: Boolean,
    val linkedFrom: Map<String, String>,
    val restrictions: Map<String, String>,

    val artists: List<SimplifiedArtist>,
    val availableMarkets: List<String>?,
    val externalUrls: Map<String, String>,
    val externalIds: Map<String, String>,

    // 新增的欄位，用於區分列表類型，例如 "CONDITION" 或 "EMOTION"
    val listType: String,



    @Embedded
    val album: AlbumDataForDb, // 將 AlbumData 的欄位攤平存入此表

    //val id: String, // 來自 SpotifyTrack 的 id

    //    val name: String,
//    val explicit: Boolean,
//    val durationMs: Int,
//    val popularity: Int,
//    val trackNumber: Int,
//    val uri: String,

    // 以下欄位將透過 TypeConverter 轉換為 JSON 字串儲存
//    val artists: List<SimplifiedArtist>,
//    val availableMarkets: List<String>?,
//    val externalUrls: Map<String, String>
    // ... 其他您想儲存的 List 或 Map 欄位
)


data class AlbumDataForDb(
    val album_id: String, // 加上前綴以避免與 TrackEntity 的 id 衝突
    val album_name: String,
    val album_type: String,
    val album_releaseDate: String,
    val album_releaseDatePrecision: String,
    val album_totalTracks: Int,
    val album_uri: String,

    // 將複雜類型轉換為 JSON String
    val album_images: String, // 原 List<SpotifyImage>
    val album_artists: String, // 原 List<SimplifiedArtist>
    val album_availableMarkets: String?, // 原 List<String>
    val album_externalUrls: String // 原 Map<String, String>
)