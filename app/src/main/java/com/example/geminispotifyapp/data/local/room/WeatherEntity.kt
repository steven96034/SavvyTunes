package com.example.geminispotifyapp.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey
    val id: Int = 0, // 固定主鍵

    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isDay: Int
)
