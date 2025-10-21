package com.example.geminispotifyapp.presentation.features.settings.aboutthisapp

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.R

@Composable
fun AboutThisAppScreen() {
    LazyColumn(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp, 0.dp, 6.dp, 12.dp)
    ) {
        item {
            Image(
                painterResource(R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo"
            )
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
        item {
            Text(
                text = "This demo App takes usage of user data from Spotify,\n" +
                        "there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(8.dp))
        }
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }
        item {
            Text("Other Statements...")
        }
    }
}