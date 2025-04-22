package com.example.geminispotifyapp.page

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.ui.theme.SpotifyBlack
import androidx.core.net.toUri

@Composable
fun TopArtistContent(topArtistsShort: List<SpotifyArtist>, topArtistsMedium: List<SpotifyArtist>, topArtistsLong: List<SpotifyArtist>, navController: NavController) {
    val scrollState = rememberScrollState()
    var expandedMenuArtist by remember { mutableStateOf(false) }
    var artistPeriodSelection by remember { mutableIntStateOf(0) }
    var onArtistSelected by remember { mutableStateOf<SpotifyArtist?>(null)}

    HandleBackToHome(navController)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                            text = "Your Top Artists",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                DropDownMenuTemplate(
                    expanded = expandedMenuArtist,
                    onExpandChange = { expandedMenuArtist = it },
                    selectedValue = artistPeriodSelection,
                    onValueChange = { artistPeriodSelection = it },
                    options = listOf("Short Term", "Medium Term", "Long Term")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (artistPeriodSelection) {
                0 -> GetTopArtists(topArtistsShort, { onArtistSelected = it })
                1 -> GetTopArtists(topArtistsMedium, { onArtistSelected = it })
                2 -> GetTopArtists(topArtistsLong, { onArtistSelected = it })
                else -> {}
            }

            Log.d("SpotifyDataContent", "Top Artists Short-Period: $topArtistsShort")
            Log.d("SpotifyDataContent", "Top Artists Medium-Period: $topArtistsMedium")
            Log.d("SpotifyDataContent", "Top Artists Long-Period: $topArtistsLong")
        }
    }
    onArtistSelected?.let { artist ->
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
                            onArtistSelected = null
                            Log.d("ArtistDetail", "Dismissing artist detail")
                        }
                    }
                }

            , // 半透明黑色背景
            contentAlignment = Alignment.Center
        ) {
            ArtistDetail(
                artist = artist,
                onDismiss = { onArtistSelected = null },
                modifier = Modifier
            )
        }
    }
}

@Composable
private fun GetTopArtists(topArtists: List<SpotifyArtist>, onArtistSelected: (SpotifyArtist) -> Unit){
    topArtists.forEachIndexed { index, artist ->
        ArtistItem(index + 1, artist, onArtistSelected)
        if (index < topArtists.size - 1
            && index < GET_ITEM_NUM -1
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
    if (topArtists.size < GET_ITEM_NUM) {
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

@Composable
private fun ArtistItem(index: Int, artist: SpotifyArtist, onArtistSelected: (SpotifyArtist) -> Unit) {

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
            val imageUrl = artist.images?.firstOrNull()?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Artist image",

                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Crop
                )
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


@Composable
fun ArtistDetail(
    artist: SpotifyArtist,
    onDismiss: (SpotifyArtist?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Surface(
        color = SpotifyBlack,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth(0.8f)
            .fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize() // Ensure Column fills the Surface
                .wrapContentSize(Alignment.TopCenter), // Align the content to top center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Artist Image
            val imageUrl = artist.images?.firstOrNull()?.url
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Artist image",

                    modifier = Modifier.clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Inside
                )
            }
            Spacer(modifier = Modifier.width(36.dp))
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Display more details about the artist here
            Text(
                text = "Popularity: ${artist.popularity}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Genres: ${artist.genres.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            val followers = artist.followers["total"]
            val formattedFollowers = followers.toString().reversed().chunked(3).joinToString(",").reversed()
            Text(
                text = "Followers: $formattedFollowers",
                style = MaterialTheme.typography.bodyMedium
            )
            val url = artist.externalUrls["spotify"]
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
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Button(onClick = { onDismiss(null) },
                modifier = Modifier.padding(16.dp)) {
                    Text(text = "Close")
                }
        }
    }
}
