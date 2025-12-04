package com.example.geminispotifyapp.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.geminispotifyapp.domain.repository.LocationTracker
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume


sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    object GpsDisabled : LocationResult()
    object MissingPermission : LocationResult()
    data class Error(val exception: Exception? = null) : LocationResult()
}

@Singleton
class LocationTrackerImpl @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application
): LocationTracker {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult {
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasAccessCoarseLocationPermission && !hasAccessFineLocationPermission) {
            return LocationResult.MissingPermission
        }

        val locationManager = application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isGpsEnabled) {
            return LocationResult.GpsDisabled
        }

        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        return suspendCancellableCoroutine { cont ->
            locationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val location = task.result
                    if (location != null) {
                        cont.resume(LocationResult.Success(location))
                    } else { // Location is null, try to get current location
                        cont.context.let {
                            locationClient.getCurrentLocation(currentLocationRequest, cont.context.let { null }).addOnCompleteListener { currentTask ->
                                if(currentTask.isSuccessful && currentTask.result != null){
                                    cont.resume(LocationResult.Success(currentTask.result))
                                } else {
                                    cont.resume(LocationResult.Error(currentTask.exception ?: Exception("Failed to get current location.")))
                                }
                            }
                        }
                    }
                } else { // Task failed
                    cont.resume(LocationResult.Error(task.exception))
                }
            }.addOnCanceledListener {
                cont.cancel()
            }
        }
    }
}