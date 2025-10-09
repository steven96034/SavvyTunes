package com.example.geminispotifyapp.features.findmusic

import android.Manifest
import android.content.Intent
import android.location.Location
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TabRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.remember
import androidx.compose.foundation.pager.rememberPagerState
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.TwoTracksList
import com.example.geminispotifyapp.UiState
import com.example.geminispotifyapp.data.SpotifyTrack
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminispotifyapp.R
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.text.DecimalFormat


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FindMusicScreen(
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    val currentWeatherData by viewModel.currentWeatherData.collectAsStateWithLifecycle()

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
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    FindMusicContent(
        currentWeatherData = currentWeatherData,
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
        getWeatherDisplayInfo = { wmoCode: Int, isDay: Boolean -> viewModel.weatherIconRepository.getWeatherDisplayInfo(wmoCode, isDay) },
        // TODO: Real refreshing function
        onRetry = {  },
        onRefresh = {  },
        isRefreshing = false
    )
}

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun FindMusicContent(
    currentWeatherData: CurrentWeatherDisplayData?,
    uiState: UiState<TwoTracksList?>,
    permissionStatus: PermissionStatus,
    location: Location?,
    showGpsDialog: Boolean,
    onFetchLocationClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onGpsDialogDismiss: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    getWeatherDisplayInfo: (wmoCode: Int, isDay: Boolean) -> Pair<String?, String?>,
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
                LazyColumn(
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

            UiState.Loading -> {
                Column (
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    currentWeatherData?.let { data ->
                        val (weatherIconUrl, weatherDescription) = getWeatherDisplayInfo(data.weatherCode, data.isDay)
                        val decimalFormat = DecimalFormat("#.0")
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(0.4f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = data.time,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row {
                                        if (weatherIconUrl != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(weatherIconUrl)
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.weather_image_placeholder)
                                                    .error(R.drawable.weather_image_placeholder)
                                                    .build(),
                                                contentDescription = weatherDescription ?: "Weather Icon",
                                                modifier = Modifier.size(24.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Text(
                                                text = " " + (weatherDescription ?: "Unknown Weather"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.weather_image_placeholder),
                                                contentDescription = "Unknown Weather",
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = " Unknown Weather",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = "Temperature",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "${decimalFormat.format(data.temperature)}°C",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator()
                }
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
                val pages = listOf("Weather", "Emotion")
                val horizontalPagerState = rememberPagerState(pageCount = { pages.size })
                val coroutineScope = rememberCoroutineScope()

                Column(modifier = Modifier.padding(bottom = 80.dp)) {
                    TabRow(selectedTabIndex = horizontalPagerState.currentPage) {
                        pages.forEachIndexed { index, title ->
                            Tab(
                                selected = horizontalPagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch {
                                        horizontalPagerState.animateScrollToPage(index)
                                    }
                                },
                                text = {
                                    Column (
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = title)
                                        Text(
                                            text = "Recommendation",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            )
                        }
                    }

                    currentWeatherData?.let { data ->
                        val (weatherIconUrl, weatherDescription) = getWeatherDisplayInfo(data.weatherCode, data.isDay)
                        val decimalFormat = DecimalFormat("#.0")
                        Card {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(0.4f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = data.time,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row {
                                        if (weatherIconUrl != null) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(weatherIconUrl)
                                                    .crossfade(true)
                                                    .placeholder(R.drawable.weather_image_placeholder)
                                                    .error(R.drawable.weather_image_placeholder)
                                                    .build(),
                                                contentDescription = weatherDescription ?: "Weather Icon",
                                                modifier = Modifier.size(24.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Text(
                                                text = " " + (weatherDescription ?: "Unknown Weather"),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(id = R.drawable.weather_image_placeholder),
                                                contentDescription = "Unknown Weather",
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                text = " Unknown Weather",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(
                                            imageVector = Icons.Default.Thermostat,
                                            contentDescription = "Temperature",
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "${decimalFormat.format(data.temperature)}°C",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalPager(state = horizontalPagerState) { pageIndex ->
                        val currentTracks = when (pageIndex) {
                            0 -> uiState.data!!.tracksA
                            1 -> uiState.data!!.tracksB
                            else -> emptyList() // Should not happen with 2 pages
                        }

                        if (currentTracks != null) {
                            val verticalPagerState = rememberPagerState(pageCount = { currentTracks.size })

                            ScrollHintVerticalPager(
                                pagerState = verticalPagerState,
                                modifier = Modifier.fillMaxSize(),
                                enableFadingEdgeHint = true,
                                enableChevronHint = true,
                                enableVerticalPagerIndicator = true, //  Enable vertical pager indicator
                                verticalPagerIndicatorActiveColor = MaterialTheme.colorScheme.primary, // Use theme primary color
                                verticalPagerIndicatorInactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), // Use theme secondary color, more transparent
                                verticalPagerIndicatorSize = 10.dp, // Slightly larger dots
                                verticalPagerIndicatorSpacing = 6.dp // More compact spacing
                            ) { page ->
                                TrackShowcase(track = currentTracks[page])
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackShowcase(
    track: SpotifyTrack
) {
    var cardColor by remember { mutableStateOf(Color.Gray) }

    Card (
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        modifier = Modifier
            .padding(4.dp)
            .fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(track.album.images.firstOrNull()?.url)
                    .crossfade(true)
                    .allowHardware(false) // Required for Palette
                    .size(Size.ORIGINAL) // Load original size to get accurate colors
                    .listener(onSuccess = { _, result ->
                        Palette.from(result.drawable.toBitmap()).generate { palette ->
                            val dominantColor = palette?.dominantSwatch?.rgb ?: return@generate
                            cardColor = Color(dominantColor)
                        }
                    })
                    .build(),
                contentDescription = "Album cover for ${track.album.name}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(0.9f)
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

            val url = track.externalUrls["spotify"]
            if (url != null) {
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        }
                    },
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollHintVerticalPager(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    enableFadingEdgeHint: Boolean = true,
    enableChevronHint: Boolean = true,
    enableVerticalPagerIndicator: Boolean = true,
    verticalPagerIndicatorModifier: Modifier = Modifier,
    verticalPagerIndicatorActiveColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
    verticalPagerIndicatorInactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    verticalPagerIndicatorSize: Dp = 8.dp,
    verticalPagerIndicatorSpacing: Dp = 8.dp,
    verticalPagerIndicatorShape: Shape = MaterialTheme.shapes.small,
    content: @Composable (page: Int) -> Unit
) {
    val backgroundColor = MaterialTheme.colorScheme.background

    val hasMorePagesBelow by remember {
        derivedStateOf { pagerState.currentPage < pagerState.pageCount - 1 }
    }

    val animatedChevronAlpha by animateFloatAsState(
        targetValue = if (hasMorePagesBelow) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "chevronAlpha"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bouncingChevron")
    val animatedBouncyOffsetY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bouncyOffsetYAnimation"
    )
    val currentOffsetY = if (hasMorePagesBelow) animatedBouncyOffsetY else 0f
    val chevronOffsetY = currentOffsetY.dp

    Box(modifier = modifier) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            content(page)
        }

        // Vertical Pager Indicator
        if (enableVerticalPagerIndicator && pagerState.pageCount > 1) {
            VerticalPagerIndicator(
                pagerState = pagerState,
                modifier = verticalPagerIndicatorModifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                activeColor = verticalPagerIndicatorActiveColor,
                inactiveColor = verticalPagerIndicatorInactiveColor,
                indicatorSize = verticalPagerIndicatorSize,
                spacing = verticalPagerIndicatorSpacing,
                indicatorShape = verticalPagerIndicatorShape
            )
        }

        // Fading Edge Hint
        if (enableFadingEdgeHint && hasMorePagesBelow) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(backgroundColor.copy(alpha = 0f), backgroundColor),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
        }

        // Subtle Chevron Hint
        if (enableChevronHint) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Swipe down to see more",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = animatedChevronAlpha),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = chevronOffsetY)
                    .padding(bottom = 16.dp)
                    .size(32.dp)
                    .alpha(animatedChevronAlpha)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerticalPagerIndicator(
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), // Color of the active dot, semi-transparent
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), // Color of inactive dots, more transparent
    indicatorSize: Dp = 8.dp, // Size of the dots
    spacing: Dp = 8.dp, // Spacing between dots
    indicatorShape: Shape = MaterialTheme.shapes.small // Shape of the dots, can be a circle or other shapes
) {
    val scope = rememberCoroutineScope() // For handling scrolling after a click

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing), // Use spacing between dots
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(pagerState.pageCount) { index ->
            val color = if (pagerState.currentPage == index) activeColor else inactiveColor
            Box(
                modifier = Modifier
                    .size(indicatorSize)
                    .clip(indicatorShape)
                    .background(color)
                    .clickable {
                        scope.launch {
                            pagerState.animateScrollToPage(index) // Scroll to the corresponding page on click
                        }
                    }
            )
        }
    }
}