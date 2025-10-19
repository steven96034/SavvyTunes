package com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.remote.model.PlayContext
import com.example.geminispotifyapp.data.remote.model.PlayHistoryObject
import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.presentation.ui.theme.GeminiSpotifyAppTheme
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyGreen
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@Composable
fun RecentlyPlayedScreen(onHistoryClick: (UiPlayHistoryObject) -> Unit, viewModel: RecentlyPlayedViewModel = hiltViewModel()) {
    val uiState by viewModel.downLoadState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val displayedRecentlyPlayed by viewModel.displayedRecentlyPlayed.collectAsState()
    val userDataNum by viewModel.userDataNum.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRecentlyPlayedIfNeeded()
    }

    RecentlyPlayedContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        displayedRecentlyPlayed = displayedRecentlyPlayed,
        onHistoryClick = onHistoryClick,
        userDataNum = userDataNum,
        onRefresh = { viewModel.refreshRecentlyPlayed() },
        onRetry = { viewModel.reFetchRecentlyPlayedIfNeeded() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyPlayedContent(
    uiState: FetchResult<List<UiPlayHistoryObject>>,
    isRefreshing: Boolean,
    displayedRecentlyPlayed: List<UiPlayHistoryObject>,
    onHistoryClick: (UiPlayHistoryObject) -> Unit,
    userDataNum: Int,
    onRefresh: () -> Unit,
    onRetry: () -> Unit
) {
    PullToRefreshBox (
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            FetchResult.Initial ->
                Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            FetchResult.Loading ->
                Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

            is FetchResult.Error -> {
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
                        if (uiState.errorData is ApiError.NetworkConnectionError)
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

            is FetchResult.Success ->
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                item {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.primary_logo_green_rgb),
                                    contentDescription = null,
                                    modifier = Modifier.height(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Recently Played Songs",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                itemsIndexed(displayedRecentlyPlayed) { index, uiPlayHistory ->
                    RecentTrackItem(index + 1, uiPlayHistory, onHistoryClick)
                    if (index < displayedRecentlyPlayed.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    } else Spacer(modifier = Modifier.height(12.dp))
                }

                if (displayedRecentlyPlayed.size < userDataNum) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You data is not enough to show more recently played songs, or try to refresh this page to get newer data. (Max = $userDataNum)")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTrackItem(index: Int, uiPlayHistory: UiPlayHistoryObject, onHistorySelected: (UiPlayHistoryObject) -> Unit) {
    val track = uiPlayHistory.originalPlayHistory.track

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
        Button(
            onClick = {
                Log.d("RecentTrackItem", "History clicked $uiPlayHistory")
                onHistorySelected(uiPlayHistory)
                 },
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Album Cover
            val thumbnailUrl =
                if (track.album.images.size >= 2)
                    track.album.images[1].url
                else
                    track.album.images.firstOrNull()?.url
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Album image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = "No album image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Song Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = track.artists.joinToString(", ") { it.name },
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )

                // Played Time
                Text(
                    text = uiPlayHistory.formattedPlayedAtTimeAgo,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview()
@Composable
fun RecentlyPlayedContentPreview() {
    val sampleTracks = List(5) { index ->
        PlayHistoryObject(
            track = SpotifyTrack(
                id = "track_id_${index + 1}",
                name = "Song Title ${index + 1}",
                artists = listOf(SimplifiedArtist(id = "artist_id_${index + 1}", name = "Artist ${index + 1}",uri = "", externalUrls = mapOf(), href = "")),
                album = SpotifyAlbum(
                    id = "album_id_${index + 1}",
                    name = "Album ${index + 1}",
                    images = listOf(),
                    releaseDate = "2023-01-01",
                    releaseDatePrecision = "day",
                    type = "album",
                    uri = "spotify:album:album_id_1",
                    availableMarkets = listOf("US", "GB"),
                    externalUrls = mapOf("spotify" to "https://open.spotify.com/album/album_id_1"),
                    totalTracks = 10 + index,
                    artists = listOf(SimplifiedArtist(id = "a1", name = "Artist 1",uri = "", externalUrls = mapOf(), href = ""))
                ),
                durationMs = 200000,
                explicit = false,
                popularity = 75,
                trackNumber = 1,
                availableMarkets = listOf("US", "GB"),
                externalUrls = mapOf("spotify" to "https://open.spotify.com/track/123"),
                externalIds = mapOf("isrc" to "US1234567890"),
                href = "https://api.spotify.com/v1/tracks/123",
                uri = "spotify:track:123",
                isPlayable = true,
                isLocal = false,
                discNumber = 1,
                linkedFrom = mapOf(),
                restrictions = mapOf(),
                type = ""
            ),
            playedAt = "2023-10-27T10:00:00.000Z",
            context = PlayContext(type = "track", uri = "spotify:track:123", externalUrls = mapOf("spotify" to "https://open.spotify.com/track/123"))
        )
    }
    val uiSampleTracks = sampleTracks.map { history ->
        UiPlayHistoryObject(
            originalPlayHistory = history,
            formattedPlayedAtTimeAgo = "Just Now", // Dummy data for preview
            formattedPlayedAtDateTime = "2023-10-27 10:00:00" // Dummy data for preview
        )
    }
    GeminiSpotifyAppTheme {
        RecentlyPlayedContent(
            uiState = FetchResult.Success(uiSampleTracks),
            isRefreshing = false,
            displayedRecentlyPlayed = uiSampleTracks,
            onHistoryClick = {},
            userDataNum = 20,
            onRefresh = {},
            onRetry = {}
        )
    }
}




@Composable
internal fun TrackHistoryDetail(
    historyTrack: UiPlayHistoryObject,
    checkMarketIfPlayable: String? = null
) {
    val track = historyTrack.originalPlayHistory.track

    // Image
    val images = track.album.images
    if (images.isNotEmpty()) {
        val pagerState = rememberPagerState(pageCount = { images.size })
        HorizontalPager(state = pagerState) { page ->
            val imageUrl = images[page].url
            AsyncImage(
                model = imageUrl,
                contentDescription = "Album image ${page + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Adjust height as needed
                    .clip(RoundedCornerShape(2.dp)),
                contentScale = ContentScale.Fit // Or ContentScale.Crop depending on desired look
            )

        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f) // Ensure the Box is a square
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)    // Placeholder background
        ) {
            Icon(
                Icons.Filled.Album,
                contentDescription = "No album image available",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(RoundedCornerShape(2.dp))
                    .align(Alignment.Center),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
    Spacer(modifier = Modifier.height(2.dp))

    // Track Name
    Text(
        text = track.name,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(2.dp))

    // Artist Name
    Text(
        text = track.artists.joinToString(", ") { it.name },
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(2.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))

    // Last Played At
    Text(
        text = "Last Played At: ${historyTrack.formattedPlayedAtDateTime}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Album Name
    Text(
        text = "From Album: ${track.album.name}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Track Number
    Text(
        text = "Track Number: ${track.trackNumber}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Release Date
    Text(
        text = "Release Date: ${track.album.releaseDate}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Track Popularity
    Text(
        text = "Track Popularity: ${track.popularity}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Track Duration
    val duration = track.durationMs.milliseconds
    val formattedDuration: String = when {
        duration < 1.minutes -> { // Less than 1 minute
            val seconds = duration.toInt(DurationUnit.SECONDS)
            String.format(Locale.getDefault(), "%02d", seconds)
        }

        duration < 1.hours -> { // Less than 1 hour
            val minutes = duration.toInt(DurationUnit.MINUTES)
            val seconds = (duration - minutes.minutes).toInt(DurationUnit.SECONDS)
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        else -> {
            val simpleDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            simpleDateFormat.format(duration)
        }
    }
    Text(
        text = "Duration: $formattedDuration ",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Is Explicit Lyrics or Not
    Text(
        text = "Explicit: ${track.explicit}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Available Markets
    if (!track.availableMarkets.isNullOrEmpty()) {
        val limit = 5
        val availableMarkets = track.availableMarkets
        var showMore by remember { mutableStateOf(false) }
        val textToShow by remember {
            derivedStateOf {
                if (showMore) {
                    availableMarkets.joinToString(", ")
                } else {
                    if (availableMarkets.size > limit) {
                        availableMarkets.take(limit).joinToString(", ")
                    } else {
                        availableMarkets.joinToString(", ")
                    }
                }
            }
        }
        val annotatedText = buildAnnotatedString {
            if (availableMarkets.size > limit)
                append("Available Markets:\n $textToShow")
            else append("Available Markets: $textToShow")
            if (availableMarkets.size > limit) {
                pushStringAnnotation(tag = "VIEW_MORE", annotation = "view_more")
                withStyle(
                    style = SpanStyle(
                        color = SpotifyGreen,
                        textDecoration = TextDecoration.Underline,
                    )
                ) {
                    append(if (showMore) "...View Less" else "...+${availableMarkets.size - limit} More")
                }
                pop()
            }
        }
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures {
                    annotatedText.getStringAnnotations(
                        tag = "VIEW_MORE",
                        start = 0,
                        end = annotatedText.length
                    ).firstOrNull()?.let {
                        showMore = !showMore
                    }
                }
            }
        )
        if (checkMarketIfPlayable != null) {
            Text(
                text = if (availableMarkets.contains(checkMarketIfPlayable)) "Playable in ${
                    checkMarketIfPlayable.let { Locale("", it).displayCountry }
                }($checkMarketIfPlayable)? Yes"
                else "Playable in ${
                    checkMarketIfPlayable.let { Locale("", it).displayCountry }
                }($checkMarketIfPlayable)? No",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    } else if (track.isPlayable != null) {
        Text(
            text = "Is Playable: ${track.isPlayable}",
            style = MaterialTheme.typography.bodyMedium
        )
    }

    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))

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
    Spacer(modifier = Modifier.height(6.dp))

    // Spotify ID
    Text(
        text = "Spotify Track ID: ${track.id}",
        style = MaterialTheme.typography.labelSmall,
    )
    Spacer(modifier = Modifier.height(4.dp))

    // External IDs
    if (track.externalIds["isrc"] != null) {
        Text(
            text = "ISRC: ${track.externalIds["isrc"]}",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (track.externalIds["ean"] != null) {
        Text(
            text = "EAN: ${track.externalIds["ean"]}",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
    if (track.externalIds["upc"] != null) {
        Text(
            text = "UPC: ${track.externalIds["upc"]}",
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Preview
@Composable
fun TrackHistoryDetailPreview() {
    val sampleTrack = PlayHistoryObject(
        track = SpotifyTrack(
            id = "track_id_1",
            name = "Sample Track",
            artists = listOf(SimplifiedArtist(id = "artist_id_1", name = "Sample Artist", uri = "", externalUrls = mapOf(), href = "")),
            album = SpotifyAlbum(
                id = "album_id_1",
                name = "Sample Album",
                images = listOf(), // No images for simplicity in preview
                releaseDate = "2023-01-01",
                releaseDatePrecision = "day",
                type = "album",
                uri = "spotify:album:album_id_1",
                availableMarkets = listOf("US", "GB", "CA", "DE", "FR", "JP", "AU"),
                externalUrls = mapOf("spotify" to "https://open.spotify.com/album/album_id_1"),
                totalTracks = 10,
                artists = listOf(SimplifiedArtist(id = "a1", name = "Artist 1", uri = "", externalUrls = mapOf(), href = ""))
            ),
            durationMs = 200000, // 3 minutes 20 seconds
            explicit = true,
            popularity = 85,
            trackNumber = 5,
            availableMarkets = listOf("US", "GB", "CA", "DE", "FR", "JP", "AU", "NZ", "IE", "SE"),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/track/sample_track_id"),
            externalIds = mapOf("isrc" to "USXYZ1234567", "ean" to "1234567890123", "upc" to "123456789012"),
            href = "https://api.spotify.com/v1/tracks/sample_track_id",
            uri = "spotify:track:sample_track_id",
            isPlayable = true,
            isLocal = false,
            discNumber = 1,
            linkedFrom = mapOf(),
            restrictions = mapOf(),
            type = "track"
        ),
        playedAt = "2023-10-27T10:30:45.123Z",
        context = PlayContext(type = "album", uri = "spotify:album:sample_album_id", externalUrls = mapOf("spotify" to "https://open.spotify.com/album/sample_album_id"))
    )

    val uiSampleTrack = UiPlayHistoryObject(
        originalPlayHistory = sampleTrack,
        formattedPlayedAtTimeAgo = "Just Now", // Dummy data for preview
        formattedPlayedAtDateTime = "2023-10-27 10:30:45" // Dummy data for preview
    )

    Box (contentAlignment = Alignment.Center) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .fillMaxHeight(0.75f)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .wrapContentSize(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TrackHistoryDetail(
                    historyTrack = uiSampleTrack
                )
            }
        }
    }
}