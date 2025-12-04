package com.example.geminispotifyapp.data.local.room

import android.util.Log
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather WHERE id = 0")
    fun getWeather(): Flow<WeatherEntity?>

    // 使用 REPLACE 策略，如果 id=0 的記錄已存在，則覆蓋它
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(weather: WeatherEntity) {
        Log.d("Dao", "upsert weather: $weather")
    }
}