package com.example.geminispotifyapp

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.geminispotifyapp.data.worker.SpotifyMetadataWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// Use Hilt to manage the dependency in whole project.
@HiltAndroidApp
class MainApplication : Application(), Configuration.Provider {

    // 1. Inject WorkManager
    @Inject
    lateinit var workManager: WorkManager

    // 2. Inject Hilt's WorkerFactory
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 3. Implement Configuration.Provider to integrate Hilt and WorkManager
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // 4. Schedule the task here
        Log.d("MainApplication", "Scheduling weekly metadata fetch")
        scheduleWeeklyMetadataFetch()
    }

    private fun scheduleWeeklyMetadataFetch() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

//        val periodicWorkRequest = PeriodicWorkRequestBuilder<SpotifyMetadataWorker>(
//            repeatInterval = 7,
//            repeatIntervalTimeUnit = TimeUnit.DAYS
//        )
//            .setConstraints(constraints)
//            .setBackoffCriteria(
//                BackoffPolicy.LINEAR,
//                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
//                TimeUnit.MILLISECONDS
//            )
//            .build()
//
//        // Use ExistingPeriodicWorkPolicy.KEEP to ensure the task is not rescheduled if it already exists.
//        // If using REPLACE, then using new settings after each app update
//        workManager.enqueueUniquePeriodicWork(
//            "SpotifyMetadataFetch",
//            ExistingPeriodicWorkPolicy.KEEP, // or REPLACE
//            periodicWorkRequest
//        )
        // *** Start of modification ***
        val workRequest = OneTimeWorkRequestBuilder<SpotifyMetadataWorker>() // Use OneTimeWorkRequestBuilder
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                // The backoff delay for OneTimeWorkRequest can usually be shorter, e.g., 10 seconds
                10,
                TimeUnit.SECONDS
            )
            .build()
        Log.d("MainApplication", "Scheduling one-time metadata fetch")
        // Use enqueueUniqueWork instead of enqueueUniquePeriodicWork
        workManager.enqueueUniqueWork(
            "SpotifyMetadataFetch", // Unique name
            ExistingWorkPolicy.REPLACE, // Replace the old task every time to ensure the new one is always executed
            workRequest
        )
    }
}