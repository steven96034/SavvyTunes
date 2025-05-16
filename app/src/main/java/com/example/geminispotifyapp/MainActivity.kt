package com.example.geminispotifyapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminispotifyapp.auth.AuthManager
import com.example.geminispotifyapp.page.MainPage


class MainActivity : ComponentActivity() {

    // Move the SpotifyRepository instance initialization to MyApplication.
//    private lateinit var spotifyRepository: SpotifyRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val appContext = this.applicationContext
//        spotifyRepository = SpotifyRepository(appContext)
        val spotifyRepository = this.app.spotifyRepository

        setContent {
            GeminiSpotifyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CheckAuthAndShowContent(spotifyRepository)
                }
            }
        }
    }
}

@Composable
fun CheckAuthAndShowContent(spotifyRepository: SpotifyRepository) {
    val context = LocalContext.current
    val accessToken by spotifyRepository.accessTokenAsFlow.collectAsState(initial = null)
    val isAuthenticated = accessToken != null

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Log.d("Auth", "User is authenticated, entering data screen.")
        } else {
            Log.d("Auth", "User is not authenticated, entering login screen.")
        }
    }

    if (!isAuthenticated && context is Activity) {
        LoginScreen(onAuthButtonClicked = { AuthManager.startAuthentication(context) })
    } else {
        SpotifyDataScreen(spotifyRepository)
    }
}

@Composable
fun LoginScreen(onAuthButtonClicked: () -> Unit) {
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

@Composable
fun SpotifyDataScreen(spotifyRepository: SpotifyRepository) {
    val context = LocalContext.current

    val viewModel: SpotifyDataScreenViewModel = viewModel()
    val downLoadState by viewModel.downLoadState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchData(spotifyRepository)
    }

    when (downLoadState) {
        DownLoadState.Initial -> {
            Log.d("SpotifyDataScreen", "Initial State")
            InitContent()
        }

        DownLoadState.Loading -> {
            Log.d("SpotifyDataScreen", "Loading State")
            LoadingContent()
        }

        is DownLoadState.Error -> {
            val errorData = (downLoadState as DownLoadState.Error).data
            if (errorData.httpStatusCode == 401) {
                AuthenticationExpiredContent(context)
            } else if (errorData.errorMessage == "Network Error") {
                NetworkErrorContent(onRetry = { viewModel.fetchData(spotifyRepository) })
            } else {
                ErrorContent(errorData.errorCause?.message, onRetry = { viewModel.fetchData(spotifyRepository) })
            }
            Log.d("SpotifyDataScreen", "Error State: $errorData")
        }

        else -> { // Success
            val successData = (downLoadState as DownLoadState.Success).data
            Log.d("SpotifyDataScreen", "Success Data: $successData")
            MainPage(successData, spotifyRepository)
        }
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun InitContent() {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
        Text("Initializing...", modifier = Modifier.align(Alignment.Center))
    }
}