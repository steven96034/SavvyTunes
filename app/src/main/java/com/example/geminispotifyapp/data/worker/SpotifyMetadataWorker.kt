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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SpotifyMetadataWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    init {
        Log.d("SpotifyWorker", "SpotifyMetadataWorker constructor called!")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        return try {
            // Cover old data: clear old data first
//            metadataDataStore.clearMetadata()
//
//            // Get new Spotify Metadata
//            val metadata = spotifyRepository.getLatestMetadata()
//
//            // Save new Metadata
//            metadataDataStore.saveMetadata(metadata)
//
//            // Send notification
//            sendNotification("Data Has Been Updated", "Click to see the latest data.")
            Log.d("SpotifyWorker", "doWork() called, but not implemented yet.")

            Result.success()
        } catch (e: Exception) {
            // If error occurs, ensure old data is deleted and retry
            //metadataDataStore.clearMetadata()
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