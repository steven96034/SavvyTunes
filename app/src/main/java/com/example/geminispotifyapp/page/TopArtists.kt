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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.geminispotifyapp.DropDownMenuTemplate
import com.example.geminispotifyapp.HandleBackToHome
import com.example.geminispotifyapp.R
import com.example.geminispotifyapp.data.SpotifyArtist

@Composable
fun TopArtistContent(topArtists: List<SpotifyArtist>, navController: NavController) {
    val scrollState = rememberScrollState()
    var expandedMenuArtist by remember { mutableStateOf(false) }
    var artistPeriodSelection by remember { mutableIntStateOf(0) }
    val batchSize = 10

    HandleBackToHome(navController)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        // 熱門藝術家
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
                        text = "您的熱門藝術家",
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


        if (topArtists.isNotEmpty()) {
            for (i in 0 until batchSize) {
                val index = i + (artistPeriodSelection * batchSize)
                if (index < topArtists.size) {
                    ArtistItem(i + 1, topArtists[index])
                    if (i < batchSize - 1) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
        Log.d("SpotifyDataContent", "Top Artists: $topArtists")
//        topArtists.forEachIndexed { index, artist ->
//            ArtistItem(index + 1, artist)
//            if (index < topArtists.size - 1) {
//                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
//            }
//        }
    }
}

@Composable
fun ArtistItem(index: Int, artist: SpotifyArtist) {
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

        // 藝術家圖片
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

        // 藝術家資訊
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "人氣指數: ${artist.popularity}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}