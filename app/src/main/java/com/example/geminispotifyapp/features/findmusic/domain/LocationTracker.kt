package com.example.geminispotifyapp.features.findmusic.domain

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume


sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    object GpsDisabled : LocationResult()
    object MissingPermission : LocationResult()
    object Error : LocationResult()
}

class LocationTracker @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    private val application: Application
){

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): LocationResult {
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

        return suspendCancellableCoroutine { cont ->
            locationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val location = task.result
                    if (location != null) {
                        cont.resume(LocationResult.Success(location))
                    } else {
                        cont.resume(LocationResult.Error)
                    }
                } else {
                    cont.resume(LocationResult.Error)
                }
            }.addOnCanceledListener {
                cont.cancel()
            }
        }
    }
}