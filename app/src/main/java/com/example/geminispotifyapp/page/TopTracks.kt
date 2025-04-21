package com.example.geminispotifyapp.page

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM


@Composable
fun TopTrackContent(topTracksShort: List<SpotifyTrack>, topTracksMedium: List<SpotifyTrack>, topTracksLong: List<SpotifyTrack>, navController: NavController) {
    val scrollState = rememberScrollState()
    var expandedMenuTrack by remember { mutableStateOf(false) }
    var trackPeriodSelection by remember { mutableIntStateOf(0) }

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
            0 -> GetTopTracks(topTracksShort)
            1 -> GetTopTracks(topTracksMedium)
            2 -> GetTopTracks(topTracksLong)
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
}

@Composable
private fun GetTopTracks(topTracks: List<SpotifyTrack>){
    topTracks.forEachIndexed { index, track ->
        TrackItem(index + 1, track)
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
private fun TrackItem(index: Int, track: SpotifyTrack) {
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
        }

        // Song Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = track.artists.joinToString(", ") { it.name },
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