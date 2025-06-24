package com.example.geminispotifyapp.features.userdatadetail.recentlyplayed

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@Composable
fun RecentlyPlayedScreen(onHistoryClick: (PlayHistoryObject) -> Unit, viewModel: RecentlyPlayedViewModel = hiltViewModel()) {
    val uiState by viewModel.downLoadState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchRecentlyPlayedIfNeeded()
    }

    RecentlyPlayedContent(uiState, isRefreshing, onHistoryClick, { viewModel.refreshRecentlyPlayed() })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyPlayedContent(
    uiState: FetchResult<List<PlayHistoryObject>>,
    isRefreshing: Boolean,
    onHistoryClick: (PlayHistoryObject) -> Unit,
    onRefresh: () -> Unit
) {
    //var onHistorySelected by remember { mutableStateOf<PlayHistoryObject?>(null) }
    Log.d("RecentlyPlayedContent", "Using Basic Box. isRefreshing=$isRefreshing")

    //HandleBackToHome(navController)

    PullToRefreshBox (
        isRefreshing = isRefreshing, // Control the visibility of refreshing indicator
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        when (uiState) {
            FetchResult.Initial -> { CircularProgressIndicator()
            Log.d("RecentlyPlayedScreen", "Initial state")}
            //TODO() // For first time loading state.

            FetchResult.Loading ->
            {CircularProgressIndicator()
                Log.d("RecentlyPlayedScreen", "Loading state")}


            is FetchResult.Error -> TODO() // Just display the error message with snackbar.

            is FetchResult.Success -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    //.padding(paddingValues)
                    .padding(horizontal = 6.dp)
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
                val recentlyPlayed = uiState.data
                itemsIndexed(recentlyPlayed) { index, playHistory ->
                    RecentTrackItem(index + 1, playHistory) { //onHistorySelected = it
                        onHistoryClick(it)
                    }
                    if (index < recentlyPlayed.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    } else Spacer(modifier = Modifier.height(12.dp))
                }

                if (recentlyPlayed.size < GET_ITEM_NUM) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You data is not enough to show more recently played songs. (Max = $GET_ITEM_NUM)")
                        }
                    }
                }
            }
        }
    }
//    DetailBox(selectedValue = onHistorySelected, onDismiss = { onHistorySelected = null }) { track, onDetailDismiss ->
//        TrackHistoryDetail(
//            historyTrack = track,
//            onDismiss = onDetailDismiss,
//        )
//    }
}

@Composable
private fun RecentTrackItem(index: Int, playHistory: PlayHistoryObject, onHistorySelected: (PlayHistoryObject) -> Unit) {
    val track = playHistory.track

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
            onClick = { onHistorySelected(playHistory) },
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
                val timeAgo = formatTimeAgo(playHistory.playedAt)
                Text(
                    text = timeAgo,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

private fun formatTimeAgo(dateString: String): String {
    try {
        // Use ISO 8601 date format to analyze the date string
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC") // 確保解析的時區為 UTC

        // Parsing the date string
        val lastPlayed = sdf.parse(dateString) ?: return "Unknown Time"

        // Get the current date
        val now = java.util.Date()

        // Calculate the time difference in seconds（1 millisecond * 1000 == 1 second）
        val diff = (now.time - lastPlayed.time) / 1000

        return when {
            diff < 60 -> "Just Now"
            diff < 60 * 60 -> "${diff / (60)} minutes ago"
            diff < 24 * 60 * 60 -> "${diff / (60 * 60)} hours ago"
            else -> "${diff / (24 * 60 * 60)} days ago"
        }
    } catch (e: Exception) {
        Log.e("TimeFormat", "Fail to parse date：$dateString", e)
        return "Unknown Time"
    }
}

private fun formatTime(dateString: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val date = sdf.parse(dateString) ?: return "Unknown Time"
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(date)
}


@Composable
internal fun TrackHistoryDetail(
    historyTrack: PlayHistoryObject,
    onDismiss: () -> Unit,
) {
    val track = historyTrack.track

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
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.padding(16.dp), shape = RectangleShape) {
            Text(text = "No Image...", style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(modifier = Modifier.height(60.dp))
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
        text = "Last Played At: ${formatTime(historyTrack.playedAt)}",
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
            }
        }) {
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Button(
            onClick = { onDismiss() },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = "Close")
        }
    }

    // Deprecated Data
//            Text(
//                text = "Is Local: ${track.isLocal}",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))

//            Text(
//                text = "Is Playable: ${track.isPlayable}",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))

//            Text(
//                text = "External URLs: ${track.externalUrls.values.joinToString(", ")}",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))

//            Text(
//                text = "URI: ${track.uri}",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))

//            val restrictions = track.restrictions["reason"]
//            Text(
//                text = if (restrictions == null) "Restrictions: None" else "Restrictions: $restrictions",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))
}