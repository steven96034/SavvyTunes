package com.example.geminispotifyapp.data.local.room

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {

    // 使用 Flow 來觀察資料變化
    @Query("SELECT * FROM tracks")
    fun getAllTracks(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)

    @Query("DELETE FROM tracks")
    suspend fun deleteAll()

    // 使用 @Transaction 確保刪除和插入是原子操作
    @Transaction
    suspend fun overwriteTracks(tracks: List<TrackEntity>) {
        deleteAll()
        insertAll(tracks)
        Log.d("Dao", "Tracks overwritten: $tracks")
    }
}