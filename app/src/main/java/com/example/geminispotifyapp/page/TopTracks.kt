package com.example.geminispotifyapp.page

import android.content.Intent
import android.icu.text.SimpleDateFormat
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.ui.theme.SpotifyBlack


@Composable
fun TopTrackContent(topTracksShort: List<SpotifyTrack>, topTracksMedium: List<SpotifyTrack>, topTracksLong: List<SpotifyTrack>, navController: NavController) {
    val scrollState = rememberScrollState()
    var expandedMenuTrack by remember { mutableStateOf(false) }
    var trackPeriodSelection by remember { mutableIntStateOf(0) }
    var onTrackSelected by remember { mutableStateOf<SpotifyTrack?>(null)}

    HandleBackToHome(navController)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
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
                selectedValue = trackPeriodSelection,
                onValueChange = { trackPeriodSelection = it },
                options = listOf("Short Term", "Medium Term", "Long Term")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (trackPeriodSelection) {
            0 -> GetTopTracks(topTracksShort) { onTrackSelected = it }
            1 -> GetTopTracks(topTracksMedium) { onTrackSelected = it }
            2 -> GetTopTracks(topTracksLong) { onTrackSelected = it }
            else -> {}
        }
//        if (topTracks.isNotEmpty()) {
//            for (i in 0 until GET_ITEM_NUM) {
//                val index = i + (trackPeriodSelection * GET_ITEM_NUM)
//                if (index < topTracks.size) {
//                    TrackItem(i + 1, topTracks[index])
//                    if (i < GET_ITEM_NUM - 1) {
//                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//                    }
//                }
//            }
//        }

        Log.d("SpotifyDataContent", "Top Tracks Short-Period: $topTracksShort")
        Log.d("SpotifyDataContent", "Top Tracks Medium-Period: $topTracksMedium")
        Log.d("SpotifyDataContent", "Top Tracks Long-Period: $topTracksLong")
    }
    onTrackSelected?.let { track ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)) // Transparent background
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val innerBoxWidth = size.width * 0.8f
                        val innerBoxHeight = size.height * 0.9f
                        val innerBoxLeft = (size.width - innerBoxWidth) / 2
                        val innerBoxTop = (size.height - innerBoxHeight) / 2

                        if (offset.x < innerBoxLeft || offset.x > innerBoxLeft + innerBoxWidth ||
                            offset.y < innerBoxTop || offset.y > innerBoxTop + innerBoxHeight
                        ) {
                            onTrackSelected = null
                            Log.d("ArtistDetail", "Dismissing artist detail")
                        }
                    }
                }

            , // 半透明黑色背景
            contentAlignment = Alignment.Center
        ) {
            TrackDetail(
                track = track,
                onDismiss = { onTrackSelected = null },
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun GetTopTracks(topTracks: List<SpotifyTrack>, onTrackSelected: (SpotifyTrack) -> Unit){
    topTracks.forEachIndexed { index, track ->
        TrackItem(index + 1, track, onTrackSelected)
        if (index < topTracks.size - 1) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
    if (topTracks.size < GET_ITEM_NUM) {
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

@Composable
private fun TrackItem(index: Int, track: SpotifyTrack, onTrackSelected: (SpotifyTrack) -> Unit) {
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
            val imageUrl = track.album.images.firstOrNull()?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Album image",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
            } else {
                Spacer(modifier = Modifier.width(72.dp))
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

@Composable
private fun TrackDetail(
    track: SpotifyTrack,
    onDismiss: (SpotifyTrack?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    Surface(
        color = SpotifyBlack,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.8f)
            .fillMaxHeight(0.9f)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize() // Ensure Column fills the Surface
                .wrapContentSize(Alignment.TopCenter), // Align the content to top center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val imageUrl = track.album.images.firstOrNull()?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Album image",
                    modifier = Modifier.clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Inside
                )
            }
            else {
                Spacer(modifier = Modifier.height(12.dp))
                Card (modifier = Modifier.padding(16.dp), shape = RectangleShape) {
                    Text(text = "No Image...", style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(modifier = Modifier.height(60.dp))
            }
            Spacer(modifier = Modifier.width(36.dp))

            Text(
                text = track.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Performing Artists: ${track.artists.joinToString(", ") { it.name }}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track From: ${track.album.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track Number: ${track.trackNumber}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Release Date: ${track.album.releaseDate}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track Popularity: ${track.popularity}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            val duration = track.durationMs
            val formattedDuration: String = when {
                duration < 60000 -> { // Less than 1 minute
                    val seconds = duration / 1000
                    String.format("%02d", seconds) + "s"
                }
                duration < 3600000 -> { // Less than 1 hour
                    val minutes = duration / 60000
                    val seconds = (duration % 60000) / 1000
                    String.format("%02d:%02d", minutes, seconds)
                }
                else -> SimpleDateFormat("HH:mm:ss").format(duration)
            }
                Text (
                text = "Duration: $formattedDuration ",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Explicit: ${track.explicit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

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

            Text(
                text = "Available Markets: ${track.availableMarkets.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

//            Text(
//                text = "External URLs: ${track.externalUrls.values.joinToString(", ")}",
//                style = MaterialTheme.typography.bodyMedium
//            )
//            Spacer(modifier = Modifier.height(4.dp))
//
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

            Text(
                text = "External IDs: ${track.externalIds.values.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))


            val url = track.externalUrls["spotify"]
            if (url != null) {
                Spacer(modifier = Modifier.height(8.dp))
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

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                Button(
                    onClick = { onDismiss(null) },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(text = "Close")
                }
            }
        }
    }
}