package com.example.geminispotifyapp.features.userdatadetail.topartists

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.data.SpotifyArtist
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.features.userdatadetail.DropDownMenuTemplate
import com.example.geminispotifyapp.features.userdatadetail.Period
import com.example.geminispotifyapp.features.userdatadetail.formatEnumPeriodName
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme


@Composable
fun TopArtistsScreen(onArtistClick: (SpotifyArtist) -> Unit, viewModel: TopArtistsViewModel = hiltViewModel()) {
    val uiState by viewModel.downLoadState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.fetchTopArtists()
    }
    TopArtistContent(uiState, onArtistClick) { viewModel.reFetchTopArtist() }
}

@Composable
fun TopArtistContent(uiState: FetchResult<TopArtistsData>, onArtistClick: (SpotifyArtist) -> Unit, onRetry: () -> Unit) { //topArtistsShort: List<SpotifyArtist>, topArtistsMedium: List<SpotifyArtist>, topArtistsLong: List<SpotifyArtist>, navController: NavController, paddingValues: PaddingValues
    var expandedMenuArtist by remember { mutableStateOf(false) }
    var artistPeriodSelection by remember { mutableStateOf(Period.SHORT_TERM) }

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

        is FetchResult.Success -> LazyColumn(
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
                                contentDescription = "Spotify Logo",
                                modifier = Modifier.height(28.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Your Top Artists",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    DropDownMenuTemplate(
                        expanded = expandedMenuArtist,
                        onExpandChange = { expandedMenuArtist = it },
                        selectedValue = artistPeriodSelection.ordinal,
                        onValueChange = { index -> artistPeriodSelection = Period.entries[index] },
                        options = Period.entries.map { formatEnumPeriodName(it) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            val currentTopArtists = when (artistPeriodSelection) {
                Period.SHORT_TERM -> uiState.data.topArtistsShort
                Period.MEDIUM_TERM -> uiState.data.topArtistsMedium
                Period.LONG_TERM -> uiState.data.topArtistsLong
            }

            itemsIndexed(currentTopArtists) { index, artist ->
                ArtistItem(index + 1, artist) { //onArtistSelected = it
                    onArtistClick(it)
                }
                if (index < currentTopArtists.size - 1) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                } else Spacer(modifier = Modifier.height(12.dp))
            }

            if (currentTopArtists.size < GET_ITEM_NUM) {
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info Icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("You data is not enough to show more artists. (Max = $GET_ITEM_NUM)")
                    }
                }
            }
        }
    }

//    DetailBox(selectedValue = onArtistSelected, onDismiss = { onArtistSelected = null }) { artist, onDetailDismiss ->
//        ArtistDetail(
//            artist = artist,
//            onDismiss = onDetailDismiss,
//        )
//    }
}

@Composable
fun ArtistItem(index: Int, artist: SpotifyArtist, onArtistSelected: (SpotifyArtist) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "$index.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp)
        )
        // Button that will show the dialog
        Button(
            onClick = { onArtistSelected(artist) },
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(0.dp)
        ) {
            // Artist Image
            val thumbnailUrl = artist.images?.size?.let {
                if (it >= 2)
                    artist.images[1].url
                else
                    artist.images.firstOrNull()?.url
            }
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = "Artist image",

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
                        Icons.Filled.Person,
                        contentDescription = "No artists image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Artist Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Popularity: ${artist.popularity}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}


@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopArtistContentPreview() {
    GeminiSpotifyAppTheme {
        val sampleArtist = SpotifyArtist(
            externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/0TnOYISbd1XYRBk9myaseg"),
            followers = mapOf("href" to null, "total" to 1000000),
            genres = listOf("Pop", "Rock"),
            href = "https://api.spotify.com/v1/artists/0TnOYISbd1XYRBk9myaseg",
            id = "0TnOYISbd1XYRBk9myaseg",
            images = listOf(),
            name = "Imagine Dragons",
            popularity = 85,
            type = "artist",
            uri = "spotify:artist:0TnOYISbd1XYRBk9myaseg"
        )
        val sampleData = TopArtistsData(
            topArtistsShort = List(5) { sampleArtist.copy(name = "Artist Short ${it + 1}") },
            topArtistsMedium = List(5) { sampleArtist.copy(name = "Artist Medium ${it + 1}") },
            topArtistsLong = List(5) { sampleArtist.copy(name = "Artist Long ${it + 1}") }
        )
        TopArtistContent(uiState = FetchResult.Success(sampleData), onArtistClick = {}, onRetry = {})
    }
}

@Composable
fun ArtistDetail(
    artist: SpotifyArtist,
    onDismiss: () -> Unit,
) {
    // Artist Image
    val images = artist.images
    val imagesSize = images?.size ?: 0
    if (imagesSize != 0) {
        val pagerState = rememberPagerState(pageCount = { imagesSize })
        HorizontalPager(state = pagerState) { page ->
            val imageUrl = images!![page].url
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
    }
    else {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .aspectRatio(1f) // Ensure the Box is a square
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant) // Placeholder background
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = "No artists image available",
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

    // Artist Name
    Text(
        text = artist.name,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))

    Spacer(modifier = Modifier.height(2.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))

    // Popularity
    Text(
        text = "Popularity: ${artist.popularity}",
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Genres
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = "Genres: ${artist.genres.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
    Spacer(modifier = Modifier.height(8.dp))

    // Followers count
    val followers = artist.followers["total"]
    val formattedFollowers = followers.toString().reversed().chunked(3).joinToString(",").reversed()
    Text(
        text = "Followers: $formattedFollowers",
        style = MaterialTheme.typography.bodyMedium
    )

    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(6.dp))

    // Open in Spotify(URL)
    val url = artist.externalUrls["spotify"]
    if (url != null) {
        val context = LocalContext.current
        Spacer(modifier = Modifier.height(8.dp))
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Button(
            onClick = { onDismiss() },
            modifier = Modifier.padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text(text = "Close")
        }
    }
}

@Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ArtistDetailPreview() {
    val sampleArtist = SpotifyArtist(
        externalUrls = mapOf("spotify" to "https://open.spotify.com/artist/0TnOYISbd1XYRBk9myaseg"),
        followers = mapOf("href" to null, "total" to 1000000),
        genres = listOf("Pop", "Rock", "Alternative"),
        href = "https://api.spotify.com/v1/artists/0TnOYISbd1XYRBk9myaseg",
        id = "0TnOYISbd1XYRBk9myaseg",
        images = listOf(),
        name = "Imagine Dragons",
        popularity = 85,
        type = "artist",
        uri = "spotify:artist:0TnOYISbd1XYRBk9myaseg"
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
                ArtistDetail(artist = sampleArtist, onDismiss = {})
            }
        }
    }
}