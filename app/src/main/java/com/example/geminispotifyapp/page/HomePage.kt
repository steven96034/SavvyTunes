package com.example.geminispotifyapp.page

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.example.geminispotifyapp.R

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun HomePage(paddingValues: PaddingValues) {
    val scrollState = rememberScrollState()

    HomeNavigation()

    Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(6.dp, 12.dp).padding(paddingValues).verticalScroll(scrollState)) {
        Column (verticalArrangement = Arrangement.Center, modifier = Modifier.padding(4.dp)) {
            Image(painterResource(R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo")
            Spacer(modifier = Modifier.padding(4.dp))
            Text(
                text = "This demo App takes usage of user data from Spotify, " +
                        "there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}

@Composable
private fun HomeNavigation() {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        }
    }

    DisposableEffect (lifecycleOwner, backDispatcher) {
        backDispatcher?.addCallback(lifecycleOwner, backCallback)
        onDispose {
            backCallback.remove()
        }
    }
}