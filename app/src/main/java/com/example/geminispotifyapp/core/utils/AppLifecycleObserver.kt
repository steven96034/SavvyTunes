package com.example.geminispotifyapp.core.utils

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.geminispotifyapp.domain.repository.FirebaseAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository
) : DefaultLifecycleObserver {

    private var lastUpdateTimestamp: Long = 0
    private val UPDATE_COOLDOWN = 60 * 60 * 1000L // 1 hr

    // Monitor App entering foreground (both Cold Start and Warm Start)
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d("AppLifecycleObserver", "App is in foreground(onStart)")
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTimestamp > UPDATE_COOLDOWN) {
            CoroutineScope(Dispatchers.IO).launch {
                firebaseAuthRepository.updateLastActiveTime()
                lastUpdateTimestamp = currentTime
            }
        }
    }
}