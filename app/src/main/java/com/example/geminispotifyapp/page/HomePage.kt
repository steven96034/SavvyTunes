package com.example.geminispotifyapp.page

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.geminispotifyapp.R

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun HomePage() {
    Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(4.dp)) {
        Column (verticalArrangement = Arrangement.Center, modifier = Modifier.padding(4.dp)) {
            Image(painterResource(R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo")
            Text("This demo App takes usage of user data from Spotify, there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!")
        }
    }
}