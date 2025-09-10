package com.example.geminispotifyapp.features.userdatadetail.toptracks

import android.annotation.SuppressLint
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.features.userdatadetail.DropDownMenuTemplate
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.features.userdatadetail.Period
import com.example.geminispotifyapp.features.userdatadetail.formatEnumPeriodName
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import java.util.Locale
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.DurationUnit

@Composable
fun TopTracksScreen(onTrackClick: (SpotifyTrack) -> Unit, viewModel: TopTracksViewModel = hiltViewModel()) {
    val uiState by viewModel.downLoadState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchTopTracks()
    }
    TopTrackContent(uiState, onTrackClick, { viewModel.reFetchTopTrack() })
}

@Composable
fun TopTrackContent(uiState: FetchResult<TopTrackData>, onTrackClick: (SpotifyTrack) -> Unit, onRetry: () -> Unit) {

    var expandedMenuTrack by remember { mutableStateOf(false) }
    var trackPeriodSelection by remember { mutableStateOf(Period.SHORT_TERM) }
    //var onTrackSelected by remember { mutableStateOf<SpotifyTrack?>(null) }

    //HandleBackToHome(navController)

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
            if (uiState.errorData is ApiError.NetworkConnectionError) {
                Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Network connection error.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text(text = "Retry")
                        }
                    }
                }
            } else {
                Box (modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "Unknown error.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text(text = "Retry")
                        }
                    }
                }
            }
        }

        is FetchResult.Success ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    //.padding(paddingValues)
                    .padding(horizontal = 6.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.primary_logo_green_rgb),
                                    contentDescription = null,
                                    modifier = Modifier.height(28.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Your Top Songs",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        DropDownMenuTemplate(
                            expanded = expandedMenuTrack,
                            onExpandChange = { expandedMenuTrack = it },
                            selectedValue = trackPeriodSelection.ordinal,
                            onValueChange = { index ->
                                trackPeriodSelection = Period.entries[index]
                            },
                            options = Period.entries.map { formatEnumPeriodName(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                val currentTopTracks = when (trackPeriodSelection) {
                    Period.SHORT_TERM -> uiState.data.topTracksShort
                    Period.MEDIUM_TERM -> uiState.data.topTracksMedium
                    Period.LONG_TERM -> uiState.data.topTracksLong
                }

                itemsIndexed(currentTopTracks) { index, track ->
                    TrackItem(index + 1, track) {
                        onTrackClick(it)
                    }
                    if (index < currentTopTracks.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    } else Spacer(modifier = Modifier.height(12.dp))
                }

                if (currentTopTracks.size < GET_ITEM_NUM) {
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Info Icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("You data is not enough to show more tracks. (Max = $GET_ITEM_NUM)")
                        }
                    }
                }
            }
    }
//    DetailBox(selectedValue = onTrackSelected, onDismiss = { onTrackSelected = null }) { track, onDetailDismiss ->
//        TrackDetail(
//            track = track,
//            onDismiss = onDetailDismiss
//        )
//    }
}

@Composable
fun TrackItem(index: Int, track: SpotifyTrack, onTrackSelected: (SpotifyTrack) -> Unit) {
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
            onClick = { onTrackSelected(track) },
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Album Cover
            val thumbnailUrl =
                if (track.album.images.size >= 2)
                    track.album.images[1].url
                else
                    track.album.images.firstOrNull()?.url // Get the second largest image for thumbnail (Spotify provides three sizes for album cover(from largest to smallest).)
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl) // Use thumbnail if available, otherwise fallback to larger image
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            else {
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
                Text(
                    text = track.album.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Preview()
@Composable
fun TopTrackContentPreview() {
    val mockTopTracks = List(5) { index ->
        SpotifyTrack(
            album = com.example.geminispotifyapp.data.SpotifyAlbum(
                artists = listOf(com.example.geminispotifyapp.data.SimplifiedArtist("artist_id_$index", "Artist Name $index", emptyMap(), "", "")),
                availableMarkets = listOf("US", "CA"),
                externalUrls = emptyMap(),
                id = "album_id_$index",
                images = listOf(),
                name = "Album Name $index",
                releaseDate = "2023-01-01",
                releaseDatePrecision = "day",
                type = "album",
                uri = "spotify:album:album_id_$index",
                totalTracks = 10
            ),
            artists = listOf(com.example.geminispotifyapp.data.SimplifiedArtist("artist_id_$index", "Artist Name $index", emptyMap(), "", "")),
            availableMarkets = listOf("US", "CA"),
            discNumber = 1,
            durationMs = 200000 + (index * 10000),
            explicit = index % 2 == 0,
            externalIds = mapOf("isrc" to "ISRC_$index"),
            externalUrls = mapOf("spotify" to "https://open.spotify.com/track/track_id_$index"),
            href = "https://api.spotify.com/v1/tracks/track_id_$index",
            id = "track_id_$index",
            isPlayable = true,
            linkedFrom = emptyMap(),
            restrictions = emptyMap(),
            name = "Track Name $index",
            popularity = 70 + index,
            trackNumber = index + 1,
            type = "track",
            uri = "spotify:track:track_id_$index",
            isLocal = false
        )
    }

    val mockTopTrackData = TopTrackData(
        topTracksShort = mockTopTracks,
        topTracksMedium = mockTopTracks.shuffled(), // Slightly different data for medium term
        topTracksLong = mockTopTracks.take(3) // Fewer items for long term
    )

    GeminiSpotifyAppTheme {
        TopTrackContent(
            uiState = FetchResult.Success(mockTopTrackData),
            onTrackClick = {},
            onRetry = {}
        )
    }
}


@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun TrackDetail(
    track: SpotifyTrack,
    onDismiss: () -> Unit,
) {
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
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
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

    // Album Release Date
    Text(
        text = "Release Date: ${track.album.releaseDate}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(4.dp))

    // Popularity
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
            val simpleDateFormat = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Button(
            onClick = { onDismiss() },
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = "Close")
        }
    }

    // Deprecated Data
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
}

@Preview(showBackground = true)
@Composable
fun TrackDetailPreview() {
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
                TrackDetail(
                    track = SpotifyTrack(
                        album = com.example.geminispotifyapp.data.SpotifyAlbum(
                            artists = listOf(com.example.geminispotifyapp.data.SimplifiedArtist("06HL4z0CvFAxyc27GXpf02", "Taylor Swift", mapOf("spotify" to "https://open.spotify.com/album/1NAmidJlEaVgA3MpcPFYGq"), "https://open.spotify.com/artist/06HL4z0CvFAxyc27GXpf02", "https://api.spotify.com/v1/artists/06HL4z0CvFAxyc27GXpf02")),
                            availableMarkets = listOf("US", "CA", "GB"),
                            externalUrls = mapOf("spotify" to "https://open.spotify.com/album/1NAmidJlEaVgA3MpcPFYGq"),
                            id = "1NAmidJlEaVgA3MpcPFYGq",
                            images = listOf(),
                            name = "Lover",
                            releaseDate = "2019-08-23",
                            releaseDatePrecision = "day",
                            type = "album",
                            uri = "spotify:album:1NAmidJlEaVgA3MpcPFYGq",
                            totalTracks = 13
                        ),
                        artists = listOf(com.example.geminispotifyapp.data.SimplifiedArtist("06HL4z0CvFAxyc27GXpf02", "Taylor Swift", mapOf("spotify" to "https://open.spotify.com/album/1NAmidJlEaVgA3MpcPFYGq"), "https://open.spotify.com/artist/06HL4z0CvFAxyc27GXpf02", "https://api.spotify.com/v1/artists/06HL4z0CvFAxyc27GXpf02")),
                        availableMarkets = listOf("US", "CA", "GB", "AU", "NZ", "DE", "FR", "ES", "IT", "JP"),
                        discNumber = 1,
                        durationMs = 200000,
                        explicit = false,
                        externalIds = mapOf("isrc" to "USUG11900001"),
                        externalUrls = mapOf("spotify" to "https://open.spotify.com/track/1dGr1c8CrMLDpV6mPbImSI"),
                        href = "https://api.spotify.com/v1/tracks/1dGr1c8CrMLDpV6mPbImSI",
                        id = "1dGr1c8CrMLDpV6mPbImSI",
                        isPlayable = true,
                        linkedFrom = mapOf("a" to "b"),
                        restrictions = mapOf("a" to "b"),
                        name = "Lover",
                        popularity = 85,
                        trackNumber = 3,
                        type = "track",
                        uri = "spotify:track:1dGr1c8CrMLDpV6mPbImSI",
                        isLocal = false
                    ),
                    onDismiss = {}
                )
            }
        }
    }
}

