package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.auth.AuthManager

@Composable
fun AuthenticationExpiredContent(context: Context) {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Authentication has expired.\n Please re-login to your Spotify account", textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    AuthManager.startAuthentication(context)
                },
                contentPadding = PaddingValues(16.dp)
            ) {
                Row {
                    Text("Connect to Spotify")
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.primary_logo_green_rgb),
                        contentDescription = null,
                        modifier = Modifier.height(20.dp)
                    )
                }
            }
            Log.d(
                "AuthenticationExpiredContent",
                "Authentication has expired. Please re-login to your Spotify account"
            )
        }
    }
}

@Composable
fun NetworkErrorContent(onRetry: () -> Unit) {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Network error",
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Please check your internet connection and try again.",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Row {
                    Text("Retry")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("🔄")
                }
            }
        }
    }
}

@Composable
fun ErrorContent(errorMessage: String?, onRetry: () -> Unit) {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Failed to load data: $errorMessage",
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}


@Preview
@Composable
fun AuthenticationExpiredContentPreview() {
    AuthenticationExpiredContent(context = LocalContext.current)
}

@Preview
@Composable
fun NetworkErrorContentPreview() {
    NetworkErrorContent(onRetry = {})
}

@Preview
@Composable
fun ErrorContentPreview() {
    ErrorContent(errorMessage = "Failed to load data", onRetry = {})
}