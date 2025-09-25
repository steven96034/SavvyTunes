package com.example.geminispotifyapp.init.login

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.geminispotifyapp.R
import androidx.core.net.toUri
import com.example.geminispotifyapp.utils.toast

@Composable
fun LoginPage(viewModel: LoginViewModel = hiltViewModel()) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val tag = "LoginPage"

    Log.d(tag, "Recomposing LoginPage. isAuthenticated: $isAuthenticated")

    LaunchedEffect(viewModel.navigateToUrlEvent) {
        viewModel.navigateToUrlEvent.collect { authUrl ->
            if (context is Activity) {
                try {
                    val customTabsIntent = CustomTabsIntent.Builder().build()
                    customTabsIntent.launchUrl(context, authUrl.toUri())
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                    context.startActivity(intent)
                }
            } else {
                Log.e(tag, "Context is not an Activity, cannot launch browser.")
                toast(context, "Error launching browser. Please try again.", Toast.LENGTH_SHORT)
            }
        }
    }

    LoginContent(
        onAuthSpotifyClick = { viewModel.onLoginClicked() },
        isAuthenticated = isAuthenticated
    )
}

@Composable
fun LoginContent(onAuthSpotifyClick: () -> Unit, isAuthenticated: Boolean, modifier: Modifier = Modifier) {
    if (!isAuthenticated) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo",
                modifier = Modifier.height(60.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text("Please connect to Spotify to continue")
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAuthSpotifyClick,
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Connect to Spotify")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = "Login with Spotify",
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview
@Composable
fun LoginPagePreview() {
    LoginContent(
        onAuthSpotifyClick = {},
        isAuthenticated = false
    )
}