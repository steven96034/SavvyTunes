package com.example.geminispotifyapp.features.findmusic

import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FindMusicScreen(
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    val wmo by viewModel.wmo.collectAsStateWithLifecycle()
    val temperature2m by viewModel.temperature2m.collectAsStateWithLifecycle()
    val allForecastTimes by viewModel.allForecastTimes.collectAsStateWithLifecycle()

    val location by viewModel.location.collectAsStateWithLifecycle()
    val showGpsDialog by viewModel.showGpsDialog.collectAsStateWithLifecycle()

    // Handle the logic after returning from the settings page
    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.fetchLocation()
    }
    val locationPermissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    FindMusicContent(
        wmo = wmo,
        temperature2m = temperature2m,
        allForecastTimes = allForecastTimes,

        permissionStatus = locationPermissionState.status,
        location = location,
        showGpsDialog = showGpsDialog,
        onFetchLocationClick = { viewModel.fetchLocation() },
        onRequestPermissionClick = { locationPermissionState.launchPermissionRequest() },
        onGpsDialogDismiss = { viewModel.onGpsDialogDismiss() },
        onOpenLocationSettingsClick = {
            viewModel.onGpsDialogDismiss()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            settingResultLauncher.launch(intent)
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FindMusicContent(
    wmo: List<Float?>?,
    temperature2m: List<Float?>?,
    allForecastTimes: List<String?>,
    permissionStatus: PermissionStatus,
    location: Location?,
    showGpsDialog: Boolean,
    onFetchLocationClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onGpsDialogDismiss: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
) {
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = onGpsDialogDismiss,
            title = { Text("Location services disabled") },
            text = { Text("To get your location, please turn on your device's location service.") },
            confirmButton = {
                TextButton(onClick = onOpenLocationSettingsClick) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = onGpsDialogDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn (
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(text = "For Test:")
        }
        item {
            if (permissionStatus.isGranted) {
                Button(onClick = onFetchLocationClick) {
                    Text("Get Location")
                }
                location?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Latitude: ${it.latitude}")
                    Text("Longitude: ${it.longitude}")
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val textToShow = if (permissionStatus.shouldShowRationale) {
                        "We need location permission to get your approximate location, please allow us access."
                    } else {
                        "Location permission is required to continue"
                    }
                    Text(textToShow)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onRequestPermissionClick) {
                        Text("Request Permission")
                    }
                }
            }
        }

        itemsIndexed(wmo ?: emptyList()) { index, wmoValue ->
            val temp = temperature2m?.getOrNull(index)
            val time = allForecastTimes.getOrNull(index)
            Row {
                Text("$time, Temp: $temp,  WMO: $wmoValue")
            }
        }
    }
}


@Composable
fun ContentScreen(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}