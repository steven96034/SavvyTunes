package com.example.geminispotifyapp

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.auth.AuthManager

fun isAuthenticationExpired(httpStatusCode: Int?) = httpStatusCode == 401

@Composable
fun AuthenticationExpiredContent(context: Context) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Authentication has expired. Please re-login to your Spotify account")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                AuthManager.startAuthentication(context)
            },
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("Connect to Spotify")
        }
        Log.d("AuthenticationExpiredContent", "Authentication has expired. Please re-login to your Spotify account")
    }
}

@Composable
fun ErrorContent(errorMessage: String?, onRetry: () -> Unit) {
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