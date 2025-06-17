package com.example.geminispotifyapp.init.checkauth

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.init.userdata.SpotifyDataScreen


@Composable
fun CheckAuth(viewModel: CheckAuthViewModel = hiltViewModel()) {
    val context = LocalContext.current
//    val accessToken by viewModel.accessToken.collectAsState(initial = null)
//    val isAuthenticated = accessToken != null
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Log.d("Auth", "User is authenticated, entering data screen.")
        } else {
            Log.d("Auth", "User is not authenticated, entering login screen.")
        }
    }

    // TODO: Improve the context check, and modify and migrate the startAuthentication(context) call to here, also check the AuthManager for some improvement.
    if (!isAuthenticated && context is Activity) {
        LoginPage(onAuthButtonClicked = { viewModel.startAuthentication() })
    } else {
        SpotifyDataScreen()
    }
}

@Composable
fun LoginPage(onAuthButtonClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Please connect to your Spotify account to continue")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAuthButtonClicked,
            contentPadding = PaddingValues(16.dp)
        ) {
            Text("Connect to Spotify")
        }
    }
}

@Preview
@Composable
fun CheckAuthPreview() {
    CheckAuth()
}

@Preview
@Composable
fun LoginPagePreview() {
    LoginPage(onAuthButtonClicked = {})
}