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
//    @Inject
//    lateinit var workManager: WorkManager

    // 2. Inject Hilt's WorkerFactory
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 3. Implement Configuration.Provider to integrate Hilt and WorkManager
    override val workManagerConfiguration: Configuration
        get() {
            Log.d("AppInit", "WorkManager is requesting Configuration from Hilt provider.")
            return Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .setMinimumLoggingLevel(Log.DEBUG)
                .build()
        }

    override fun onCreate() {
        super.onCreate()
        // ✅ 關鍵：手動使用 Hilt 準備好的 workerFactory 來初始化 WorkManager
        // 這個流程是線性的，沒有循環
//        val config = Configuration.Builder()
//            .setWorkerFactory(workerFactory)
//            .setMinimumLoggingLevel(Log.DEBUG)
//            .build()
//        WorkManager.initialize(this, config)

        Log.d("AppInit", "MainApplication.onCreate() started.")

//        try {
//            // 檢查 Hilt 是否真的注入了 workerFactory
//            if (!::workerFactory.isInitialized) {
//                Log.e("AppInit", "HiltWorkerFactory was NOT injected by Hilt! Hilt setup is broken.")
//                // 如果 Hilt 注入失敗，後面的程式碼也沒有意義了
//                return
//            }
//            Log.d("AppInit", "HiltWorkerFactory injected successfully: $workerFactory")
//
//            // 建立設定
//            val config = Configuration.Builder()
//                .setWorkerFactory(workerFactory)
//                .setMinimumLoggingLevel(android.util.Log.DEBUG)
//                .build()
//            Log.d("AppInit", "WorkManager Configuration created.")
//
//            // 進行手動初始化
//            WorkManager.initialize(this, config)
//            Log.d("AppInit", "WorkManager.initialize() called successfully.")
//
//        } catch (e: IllegalStateException) {
//            // 這是關鍵的捕捉！如果 WorkManager 已經被初始化，這裡會拋出異常
//            Log.e("AppInit", "CRITICAL ERROR: WorkManager was already initialized before manual setup.", e)
//        } catch (e: Exception) {
//            Log.e("AppInit", "An unexpected error occurred during initialization.", e)
//        }

        // 4. Schedule the task here
        Log.d("MainApplication", "Scheduling weekly metadata fetch")
        //scheduleWeeklyMetadataFetch()
    }

    private fun scheduleWeeklyMetadataFetch() {
        // ✅ 關鍵：透過標準的 getInstance() 方法獲取已被正確設定的 WorkManager 單例
//        val workManager = WorkManager.getInstance(this)

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
            .setInitialDelay(15, TimeUnit.SECONDS) // Add 20 seconds initial delay
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
//        workManager.enqueueUniqueWork(
//            "SpotifyMetadataFetch", // Unique name
//            ExistingWorkPolicy.REPLACE, // Replace the old task every time to ensure the new one is always executed
//            workRequest
//        )
    }
}

//@HiltAndroidApp
//// ✅ 關鍵：回到並實現 Configuration.Provider
//class MainApplication : Application(), Configuration.Provider {
//
//    // ✅ Hilt 會在 Application 建立時注入這個工廠
//    @Inject
//    lateinit var workerFactory: HiltWorkerFactory
//
//    // 🔴 關鍵：【不要】注入 WorkManager，以避免任何潛在的初始化衝突
//    // @Inject
//    // lateinit var workManager: WorkManager
//
//    // ✅ 關鍵：覆寫 workManagerConfiguration 屬性
//    // 這是 WorkManager 在需要初始化時，回來向您請求設定的唯一入口
//    override val workManagerConfiguration: Configuration
//        get() {
//            Log.d("AppInit", "WorkManager is requesting Configuration from our Hilt Provider.")
//            return Configuration.Builder()
//                .setWorkerFactory(workerFactory)
//                .setMinimumLoggingLevel(Log.DEBUG)
//                .build()
//        }
//
//    override fun onCreate() {
//        super.onCreate()
//        Log.d("AppInit", "MainApplication.onCreate() called.")
//        // 🔴 關鍵：【不要】在這裡進行任何手動初始化
//        scheduleWorkRequest()
//    }
//
//    private fun scheduleWorkRequest() {
//        // ✅ 關鍵：總是使用標準的 `getInstance()` 方法來獲取 WorkManager
//        // 第一次呼叫它時，它會自動觸發上面的 `workManagerConfiguration`
//        val workManager = WorkManager.getInstance(this)
//
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        val workRequest = OneTimeWorkRequestBuilder<SpotifyMetadataWorker>()
//            .setInitialDelay(10, TimeUnit.SECONDS)
//            .setConstraints(constraints)
//            .build()
//
//        Log.d("AppInit", "Enqueuing work request using WorkManager.getInstance().")
//        workManager.enqueueUniqueWork(
//            "SpotifyMetadataFetch",
//            ExistingWorkPolicy.REPLACE,
//            workRequest
//        )
//    }
//}