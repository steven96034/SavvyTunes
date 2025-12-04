package com.example.geminispotifyapp.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.geminispotifyapp.MainActivity
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.local.room.AppLocalDataSource
import com.example.geminispotifyapp.domain.usecase.FindWeatherRelatedMusic
import com.example.geminispotifyapp.domain.usecase.GetLocationAndWeatherUseCase
import com.example.geminispotifyapp.presentation.features.main.home.HomeDataState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.last

@HiltWorker
class SpotifyMetadataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val getLocationAndWeatherUseCase: GetLocationAndWeatherUseCase,
    private val findWeatherRelatedMusicUseCase: FindWeatherRelatedMusic,
    //private val localDataSource: AppLocalDataSource
) : CoroutineWorker(appContext, workerParams) {

    init {
        Log.d("SpotifyWorker", "SpotifyMetadataWorker constructor called!")
    }

    @RequiresPermission(allOf = [Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACCESS_COARSE_LOCATION])
    override suspend fun doWork(): Result {
        var weatherState: HomeDataState = HomeDataState.Initial
        return try {
            // TODO：Cover old data: clear old data first
//            metadataDataStore.clearMetadata()

//            if (weatherState !is HomeDataState.Success) {
//                weatherState = getLocationAndWeatherUseCase()
//            }
//
//            if (weatherState is HomeDataState.Success) {
//                val finalState = findWeatherRelatedMusicUseCase(weatherState.data.weatherResponse).last()
//                val weatherData = weatherState.data.weatherResponse
//
//                Log.d("SpotifyWorker", "Music Recommendation Final State: $finalState")
//
//                when (finalState) {
//                    is UiState.Success -> {
//                        // TODO：Save data from finalState.data and weatherState.data.weatherResponse
//                        //localDataSource.saveTracks(finalState.data)
//                        //localDataSource.saveWeather(weatherData)
//                        Log.d("SpotifyWorker", "Music Recommendation Refresh Success(Weather and Tracks")
//                    }
//                    is UiState.Error -> {
//                        throw Exception(finalState.message)
//                    }
//                    is UiState.Loading, is UiState.Initial -> {
//                        throw Exception("Flow completed prematurely without a final result.")
//                    }
//                }
//            }
//            else {
//                throw Exception("Weather state is not success, try again below.")
//            }

//            // Send notification
            sendNotification("Data Has Been Updated", "Click to see the latest data.")
            Log.d("SpotifyWorker", "doWork() called, but only implemented parts.")

            Result.success()
        } catch (e: Exception) {
            // If error occurs, ensure old data is deleted and retry
            //metadataDataStore.clearMetadata()
            Log.d("SpotifyWorker", "runAttemptCount: $runAttemptCount")
            if (runAttemptCount < 3) { // limit to 3 attempts
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun sendNotification(title: String, message: String) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        val channelId = "spotify_metadata_channel"

        val channel = NotificationChannel(
            channelId,
            "Spotify Metadata Updates",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.app_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(1, notification)
        } else {
            // Not send notification if permission not granted, but metadata should be updated.
            Log.d("SpotifyWorker", "Cannot send notification, permission not granted.")
        }
    }
}