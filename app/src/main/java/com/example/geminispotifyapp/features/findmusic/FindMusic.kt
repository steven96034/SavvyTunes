package com.example.geminispotifyapp.features.findmusic

import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.TwoTracksList
import com.example.geminispotifyapp.UiState
import com.example.geminispotifyapp.data.SpotifyTrack
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geminispotifyapp.R
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

    val uiState by viewModel.findWeatherMusicUiState.collectAsStateWithLifecycle()


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

        uiState = uiState,
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
        },
        // TODO: Real refreshing function
        onRetry = {  },
        onRefresh = {  },
        isRefreshing = false
    )
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun FindMusicContent(
    wmo: List<Float?>?,
    temperature2m: List<Float?>?,
    allForecastTimes: List<String?>,
    uiState: UiState<TwoTracksList>,
    permissionStatus: PermissionStatus,
    location: Location?,
    showGpsDialog: Boolean,
    onFetchLocationClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onGpsDialogDismiss: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean
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

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            UiState.Initial ->
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
                }
            UiState.Loading ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (uiState.throwable is ApiError.NetworkConnectionError)
                            Text(text = "Network connection error.", textAlign = TextAlign.Center)
                        else
                            Text(text = "Error.", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text(text = "Retry")
                        }
                    }
                }
            }

            is UiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    if (uiState.data.tracksA != null) {
                        item {
                            val pagerState = rememberPagerState(pageCount = {
                                uiState.data.tracksA.size
                            })
                            HorizontalPager(state = pagerState) { page ->
                                TrackShowcase(track = uiState.data.tracksA[page])
                            }
                        }
                    }
                    item {
                        HorizontalDivider()
                    }
                    if (uiState.data.tracksB != null) {
                        item {
                            val pagerState = rememberPagerState(pageCount = {
                                uiState.data.tracksB.size
                            })
                            HorizontalPager(state = pagerState) { page ->
                                TrackShowcase(track = uiState.data.tracksB[page])
                            }
                        }
                    }
                }
            }
        }
    }


//        itemsIndexed(wmo ?: emptyList()) { index, wmoValue ->
//            val temp = temperature2m?.getOrNull(index)
//            val time = allForecastTimes.getOrNull(index)
//            Row {
//                Text("$time, Temp: $temp,  WMO: $wmoValue")
//            }
//        }
//    }
}

@Composable
fun TrackShowcase(
    track: SpotifyTrack
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(track.album.images.firstOrNull()?.url)
                .crossfade(true)
                .build(),
            contentDescription = "Album cover for ${track.album.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(bottom = 16.dp)
        )

        Text(
            text = track.name,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = track.artists.joinToString(", ") { it.name },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        // Open in Spotify (URL)
        val url = track.externalUrls["spotify"]
        if (url != null) {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(4.dp))
            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                } },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row {
                    Text(text = "Open in Spotify")
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.primary_logo_green_rgb),
                        contentDescription = null,
                        modifier = Modifier.height(20.dp)
                    )
                }
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