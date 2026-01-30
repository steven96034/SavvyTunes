package com.example.geminispotifyapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.geminispotifyapp.MainActivity
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var firebaseAuthRepository: FirebaseAuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")

        serviceScope.launch {
            try {
                firebaseAuthRepository.updateFcmToken(token)
                Log.d("FCM", "Token synced to Firestore successfully.")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to sync token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"]
            if (type == "daily_recommendation") {
                val title = remoteMessage.data["title"] ?: "Savvy Tunes"
                val body = remoteMessage.data["body"] ?: "New music arrived!"

                showNotification(title, body)
                Log.d("FCM", "Daily recommendation notification received.")
            }
        }
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "daily_recommendation_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Daily Recommendations",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}