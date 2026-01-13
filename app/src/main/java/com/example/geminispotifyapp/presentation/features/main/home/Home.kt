package com.example.geminispotifyapp.presentation.features.main.home

import android.Manifest
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.remember
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Recommend
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Size
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.remote.model.TrackFromCloudRecommendation
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyGrey
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyWhite
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import kotlin.math.abs


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val currentWeatherData by viewModel.weatherDataJson.collectAsStateWithLifecycle()

    val showGpsDialog by viewModel.showGpsDialog.collectAsStateWithLifecycle()

    val uiState by viewModel.findWeatherMusicUiState.collectAsStateWithLifecycle()
    val recommendationUiState by viewModel.recommendationUiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val showBottomSheet by viewModel.showBottomSheet.collectAsStateWithLifecycle()

    // Detect portrait orientation
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    // Handle the logic after returning from the settings page
    val settingResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.fetchLocationAndWeather()
    }
    val locationPermissionState = rememberPermissionState(
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    LaunchedEffect(locationPermissionState.status, uiState) {
        // Check if the location permission is granted and the UI state is Initial then fetch location
        if (locationPermissionState.status.isGranted && uiState is UiState.Initial) {
            viewModel.fetchLocationAndWeather()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    // --- Modal Bottom Sheet ---
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setBottomSheetVisibility(false) },
            sheetState = sheetState,
            modifier = Modifier.fillMaxHeight(0.95f)
        ) {
            RecommendationSheetContent(uiState = recommendationUiState, onRefresh = { viewModel.fetchLatestRecommendation() })
        }
    }
    Scaffold (floatingActionButton = {
        FloatingButton(
            onClick = {
                viewModel.fetchLatestRecommendation()
                viewModel.setBottomSheetVisibility(true)
            },
            modifier = Modifier.padding(bottom = 80.dp)
        )
    }) { paddingValues ->
        HomeContent(
            currentWeatherData = currentWeatherData,
            uiState = uiState,
            isPortrait = isPortrait,
            permissionStatus = locationPermissionState.status,
            showGpsDialog = showGpsDialog,
            onRequestPermissionClick = { locationPermissionState.launchPermissionRequest() },
            onGpsDialogDismiss = { viewModel.onGpsDialogDismiss() },
            onOpenLocationSettingsClick = {
                viewModel.onGpsDialogDismiss()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                settingResultLauncher.launch(intent)
            },
            getWeatherDisplayInfo = { wmoCode: Int, isDay: Boolean -> viewModel.weatherIconRepository.getWeatherDisplayInfo(wmoCode, isDay) },
            onRetry = { viewModel.fetchLocationAndWeather() },
            onRefresh = { viewModel.refreshHome() },
            isRefreshing = isRefreshing,
            navigateToSettings = { viewModel.navigateToSettings() },
            paddingValues = paddingValues
        )
    }
}

@OptIn(
    ExperimentalPermissionsApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class
)
@Composable
fun HomeContent(
    currentWeatherData: WeatherResponse?,
    uiState: UiState<TwoTracksList?>,
    isPortrait: Boolean,
    permissionStatus: PermissionStatus,
    showGpsDialog: Boolean,
    onRequestPermissionClick: () -> Unit,
    onGpsDialogDismiss: () -> Unit,
    onOpenLocationSettingsClick: () -> Unit,
    getWeatherDisplayInfo: (wmoCode: Int, isDay: Boolean) -> Pair<String?, String?>,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    navigateToSettings: () -> Unit,
    paddingValues: PaddingValues
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

    var hasRequestedPermissionAtLeastOnce by remember { mutableStateOf(false) }
    val context = LocalContext.current

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            UiState.Initial ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (permissionStatus.isGranted) {
                        CircularProgressIndicator()
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // Key logic check:
                            // If shouldShowRationale is false, and we have "requested at least once",
                            // it means the user has "permanently denied" the permission.
                            val isPermanentlyDenied = !permissionStatus.shouldShowRationale && hasRequestedPermissionAtLeastOnce

                            if (isPermanentlyDenied) {
                                // Situation 3: Permanently Denied -> Open Settings
                                Text(
                                    "You have permanently denied location permission. Please open it in the app settings to continue using this feature.",
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                }) {
                                    Text("Go to Settings")
                                }
                            } else {
                                val textToShow = if (permissionStatus.shouldShowRationale) {
                                    // Situation 2: The second time asking for permission, explain why the permission is required.
                                    "We need location permission to get your approximate location, so we can provide weather information and music recommendation, please allow us access."
                                } else {
                                    // Situation 1: First time of asking for permission.
                                    "Location permission is required to continue."
                                }
                                Text(textToShow, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    hasRequestedPermissionAtLeastOnce = true
                                    onRequestPermissionClick()
                                }) {
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
                    val infiniteTransition = rememberInfiniteTransition(label = "loading_color_transition")
                    val animatedColor by infiniteTransition.animateColor(
                        initialValue = Color(0xFF00796B), // Teal
                        targetValue = Color(0xFF7B1FA2), // Purple
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ), label = "loading_color_animation"
                    )

                    currentWeatherData?.current?.let { data ->
                        val (weatherIconUrl, weatherDescription) = getWeatherDisplayInfo(data.weatherCode, data.isDay == 1)
                        val decimalFormat = DecimalFormat("#.0")
                        Card (
                            colors = CardDefaults.cardColors(
                                containerColor = animatedColor
                            ),
                            modifier = Modifier
                                .padding(4.dp)
                                .fillMaxSize()
                                .align(Alignment.CenterHorizontally),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = data.time,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (weatherIconUrl != null) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(weatherIconUrl)
                                                .crossfade(true)
                                                .placeholder(R.drawable.weather_image_placeholder)
                                                .error(R.drawable.weather_image_placeholder)
                                                .build(),
                                            contentDescription = weatherDescription
                                                ?: "Weather Icon",
                                            modifier = Modifier.size(48.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            text = " " + (weatherDescription
                                                ?: "Unknown Weather"),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.weather_image_placeholder),
                                            contentDescription = "Unknown Weather",
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = " Unknown Weather",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Icon(
                                        imageVector = Icons.Default.Thermostat,
                                        contentDescription = "Temperature",
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "${decimalFormat.format(data.temperature)}°C",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator()
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var dotCount by remember { mutableIntStateOf(0) }
                                    LaunchedEffect(Unit) {
                                        while (true) {
                                            delay(300)
                                            dotCount = (dotCount % 3) + 1
                                        }
                                    }
                                    val animatedDotCount by animateIntAsState(
                                        targetValue = dotCount,
                                        label = "dotAnimation"
                                    )
                                    Text(text = "Loading" + ".".repeat(animatedDotCount))
                                }
                            }
                        }
                    }
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
                                    if (isPortrait)
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
                                    else {
                                        Row (
                                            modifier = Modifier.fillMaxSize(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "Recommendation for ",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 2.dp).padding(start = 4.dp)
                                            )
                                            Text(
                                                text = title,
                                                modifier = Modifier.padding(vertical = 2.dp).padding(end = 4.dp)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }

                    currentWeatherData?.current?.let { data ->
                        val (weatherIconUrl, weatherDescription) = getWeatherDisplayInfo(
                            data.weatherCode,
                            data.isDay == 1
                        )
                        val decimalFormat = DecimalFormat("#.0")
                        Card(modifier = Modifier.padding(4.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = data.time,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Spacer(modifier = Modifier.width(16.dp))
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

                    HorizontalPager(state = horizontalPagerState) { pageIndex ->
                        val currentTracks = when (pageIndex) {
                            0 -> uiState.data!!.tracksA
                            1 -> uiState.data!!.tracksB
                            else -> emptyList() // Should not happen with 2 pages
                        }
                        Log.d("HomeContent", "currentTracks: $currentTracks")
                        if (!currentTracks.isNullOrEmpty()) { // If the list is not empty or null.
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
                                TrackShowcase(track = currentTracks[page], isPortrait = isPortrait)
                            }
                        } else {
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
                                    Text(
                                        "Recommendation is not found in this category of searching result.",
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val fullMessage = // The complete message after translation
                                            "You can add the number of showcase to search or adjust other filters in Settings Page."
                                        val settingsWord = "Settings Page"
                                        val startIndex = fullMessage.indexOf(settingsWord)
                                        val endIndex = startIndex + settingsWord.length
                                        val annotatedString = buildAnnotatedString {
                                            // First, add the complete message to the AnnotatedString
                                            append(fullMessage)
                                            if (startIndex != -1) {
                                                // Use the addLink function to add a clickable LinkAnnotation
                                                addLink(
                                                    // LinkAnnotation.Clickable defines the click behavior and style
                                                    clickable = LinkAnnotation.Clickable(
                                                        tag = "settings_link_tag",
                                                        styles = TextLinkStyles(
                                                            SpanStyle(
                                                                color = MaterialTheme.colorScheme.primary,
                                                                textDecoration = TextDecoration.Underline
                                                            )
                                                        ),
                                                        linkInteractionListener = { navigateToSettings() }
                                                    ),
                                                    start = startIndex,
                                                    end = endIndex
                                                )
                                            }
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Info",
                                            modifier = Modifier
                                                .padding(horizontal = 8.dp)
                                                .size(16.dp)
                                        )
                                        Text(
                                            text = annotatedString,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(end = 8.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Wanna try again?", textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = onRefresh) { // Retry for user but actually refresh for app
                                        Text(text = "Retry")
                                    }
                                }
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
    track: SpotifyTrack,
    isPortrait: Boolean
) {
    var imageWidthFraction by remember { mutableStateOf(0.9f) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var actualImageWidth by remember { mutableStateOf(0) }
    var actualImageHeight by remember { mutableStateOf(0) }
    val imageAspectRatio by remember(actualImageWidth, actualImageHeight) {
        derivedStateOf {
            if (actualImageWidth > 0 && actualImageHeight > 0) {
                actualImageWidth.toFloat() / actualImageHeight.toFloat()
            } else {
                1f // Default value until actual dimensions are available
            }
        }
    }


    var cardColor by remember { mutableStateOf(Color.Gray) }

    val trackTextColor = remember(cardColor, MaterialTheme.colorScheme.onSurface) {
        val onSurfaceLuminance = SpotifyWhite.luminance()
        val cardLuminance = cardColor.luminance()
        if (abs(onSurfaceLuminance - cardLuminance) < 0.2f) {
            if (onSurfaceLuminance > 0.5f) Color.Black else Color.White
        } else {
            SpotifyWhite
        }
    }

    val artistTextColor = remember(cardColor, MaterialTheme.colorScheme.onSurfaceVariant) {
        val onSurfaceVariantLuminance = SpotifyGrey.luminance()
        val cardLuminance = cardColor.luminance()
        // If the luminance difference is small, choose a contrasting color.
        if (abs(onSurfaceVariantLuminance - cardLuminance) < 0.2f) {
            if (cardLuminance > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.7f)
        } else {
            SpotifyGrey
        }
    }

    Card (
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        modifier = Modifier
            .onSizeChanged { cardSize = it }
            .padding(horizontal = 4.dp)
            .fillMaxSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (isPortrait) { // For portrait orientation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LaunchedEffect(cardSize, imageAspectRatio) {
                    if (cardSize.height > 0 && cardSize.width > 0 && imageAspectRatio > 0 && actualImageWidth > 0 && actualImageHeight > 0) {
                        Log.d("TrackShowcase", "(${track.name}) current cardSize: $cardSize, imageAspectRatio: $imageAspectRatio")
                        Log.d("TrackShowcase", "(${track.name}) Actual image dimensions: ${actualImageWidth}x${actualImageHeight}")
                        // Goal: Image height should not exceed 70% of the card's height
                        val maxAllowedImageHeight = cardSize.height * 0.7f

                        // If the image were displayed at 100% card width, its height would be (cardSize.width / imageAspectRatio)
                        // We need to find a fraction (f) such that (cardSize.width * f) / imageAspectRatio <= maxAllowedImageHeight

                        // Back-calculate the maximum allowed image width from the height constraint
                        val maxWidthFromHeightConstraint = maxAllowedImageHeight * imageAspectRatio

                        // Now calculate the ratio of this width to the total card width
                        val fractionBasedOnHeight = maxWidthFromHeightConstraint / cardSize.width

                        // We also need to consider that the image cannot exceed the card's width itself (i.e., fraction <= 1.0f)
                        // and we want a maximum of 0.9f (based on the initial value)
                        // and a minimum of 0.2f
                        val calculatedFraction = fractionBasedOnHeight
                            .coerceAtMost(0.9f) // Cannot exceed 90% of the card's width
                            .coerceAtLeast(0.2f) // Cannot be less than 20% of the card's width

                        // Only update the state if the value has actually changed to avoid unnecessary recomposition
                        if (abs(calculatedFraction - imageWidthFraction) > 0.001f) { // Use a small threshold to compare floating-point numbers
                            imageWidthFraction = calculatedFraction
                            Log.d("TrackShowcase", "(${track.name}) Updated imageWidthFraction to: $imageWidthFraction")
                        }
                    } else {
                        // If size information is incomplete, set a reasonable default value
                        if (imageWidthFraction != 0.9f) {
                            imageWidthFraction = 0.9f
                        }
                    }
                }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(track.album.images.firstOrNull()?.url)
                        .crossfade(true)
                        .allowHardware(false) // Required for Palette
                        .size(Size.ORIGINAL) // Load original size to get accurate colors
                        .listener(onSuccess = { _, result ->
                            actualImageWidth = result.drawable.intrinsicWidth
                            actualImageHeight = result.drawable.intrinsicHeight
                            Palette.from(result.drawable.toBitmap()).generate { palette ->
                                val targetSwatch = palette?.dominantSwatch ?: palette?.mutedSwatch ?: palette?.vibrantSwatch
                                val dominantColor = targetSwatch?.rgb ?: return@generate
                                cardColor = Color(dominantColor)
                            }
                        })
                        .build(),
                    contentDescription = "Album cover for ${track.album.name}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(imageWidthFraction)
                        .padding(bottom = 16.dp)
                )

                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = trackTextColor,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artists.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = artistTextColor,
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = artistTextColor.copy(
                                alpha = 0.3f
                            ), contentColor = trackTextColor
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 0.dp
                        )
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
        else { // For landscape orientation
            Row (
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
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
                        .fillMaxWidth(0.5f)
                        .padding(bottom = 16.dp)
                )
                Column (
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = track.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = trackTextColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.artists.joinToString(", ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = artistTextColor,
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = artistTextColor.copy(
                                    alpha = 0.3f
                                ), contentColor = trackTextColor
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 2.dp,
                                pressedElevation = 0.dp
                            )
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

@Composable
fun RecommendationSheetContent(uiState: RecommendationUiState, onRefresh: () -> Unit) {
    when (uiState) {
        is RecommendationUiState.Loading -> {
            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is RecommendationUiState.Empty -> {
            Box(Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) {
                Column (
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "So far there is no recommendation.",
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Maybe you will get one tomorrow!",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        is RecommendationUiState.Error -> {
            Box(Modifier.fillMaxWidth().height(500.dp), contentAlignment = Alignment.Center) {
                Column (
                    modifier = Modifier.fillMaxWidth(0.8f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Error occurred: ${uiState.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRefresh) {
                        Text("Retry for daily recommendation.")
                    }
                }
            }
        }
        is RecommendationUiState.Success -> {
            val recommendation = uiState.data

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = "📅  ${recommendation.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.summary,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(recommendation.tracks) { track ->
                        RecommendationTrackItem(track = track)
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationTrackItem(track: TrackFromCloudRecommendation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val context = LocalContext.current
        Spacer(modifier = Modifier.height(4.dp))
        IconButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, track.uri.toUri())
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play")
        }
    }
}

@Composable
fun FloatingButton(onClick: () -> Unit, modifier: Modifier) {
    SmallFloatingActionButton(
        onClick = { onClick() },
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary,
        modifier = modifier
    ) {
        Icon(Icons.Filled.Recommend, "Small floating action button of recommendation.")
    }
}