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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.geminispotifyapp.auth.AuthManager
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.page.MainPage
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import retrofit2.HttpException
import java.io.IOException


class MainActivity : ComponentActivity() {

    private lateinit var spotifyDataManager: SpotifyDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SpotifyDataManager
        spotifyDataManager = SpotifyDataManager(this)

        setContent {
            GeminiSpotifyAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    CheckAuthAndShowContent(spotifyDataManager)
                }
            }
        }
    }
}

@Composable
fun CheckAuthAndShowContent(spotifyDataManager: SpotifyDataManager) {
    val context = LocalContext.current
    val accessToken by spotifyDataManager.accessTokenAsFlow.collectAsState(initial = null)
    val isAuthenticated = accessToken != null

    // Using LaunchedEffect to handle side effects, like showing logs or initiating some actions once the value changes.
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            Log.d("Auth", "User is authenticated, entering data screen.")
        } else {
            Log.d("Auth", "User is not authenticated, entering login screen.")
        }
    }

    // Separating Composable functions for better readability and testing.
    if (!isAuthenticated && context is Activity) {
        LoginScreen(onAuthButtonClicked = { AuthManager.startAuthentication(context) })
    } else {
        SpotifyDataScreen(spotifyDataManager)
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
fun SpotifyDataScreen(spotifyDataManager: SpotifyDataManager) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Use a data class to manage the screen's state
    var screenState by remember { mutableStateOf(ScreenState()) }

    // Define a function to fetch data
    // Inner function is brilliant for data manipulation along with layout and also tidy for coding
    fun fetchData() {
        scope.launch {
            screenState = screenState.copy(isLoading = true, httpStatusCode = null ,errorMessage = null, errorCause = null)
            try {
                // Use supervisorScope to ensure that a single task fails doesn't cancel other tasks
                supervisorScope {
                    Log.d("SpotifyDataScreen", "Fetching data...")
//                spotifyDataManager.refreshTokenDataIfNeeded()

                    // Use async to fetch data in parallel
                    val topArtistsDeferredShort = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopArtists(
                            timeRange = "short_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topArtistDeferredMedium = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopArtists(
                            timeRange = "medium_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topArtistsDeferredLong = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopArtists(
                            timeRange = "long_term",
                            limit = GET_ITEM_NUM
                        )
                    }

                    val topTracksDeferredShort = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopTracks(
                            timeRange = "short_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredMedium = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopTracks(
                            timeRange = "medium_term",
                            limit = GET_ITEM_NUM
                        )
                    }
                    val topTracksDeferredLong = async(Dispatchers.IO) {
                        spotifyDataManager.getUserTopTracks(
                            timeRange = "long_term",
                            limit = GET_ITEM_NUM
                        )
                    }

                    val recentlyPlayedDeferred = async(Dispatchers.IO) {
                        spotifyDataManager.getRecentlyPlayedTracks(limit = GET_ITEM_NUM)
                    }

                    // Await all the deferred values
                    val topArtistsShort = try {
                        topArtistsDeferredShort.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topArtistsMedium = try {
                        topArtistDeferredMedium.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topArtistsLong = try {
                        topArtistsDeferredLong.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }

                    val topTracksShort = try {
                        topTracksDeferredShort.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topTracksMedium = try {
                        topTracksDeferredMedium.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }
                    val topTracksLong = try {
                        topTracksDeferredLong.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }

                    val recentlyPlayed = try {
                        recentlyPlayedDeferred.await().items
                    } catch (e: Exception) {
                        Log.d("SpotifyDataScreen", "Catch and Throw: $e")
                        throw e
                    }


                    screenState = screenState.copy(
                        isLoading = false,
                        topArtistsShort = topArtistsShort,
                        topArtistsMedium = topArtistsMedium,
                        topArtistsLong = topArtistsLong,
                        topTracksShort = topTracksShort,
                        topTracksMedium = topTracksMedium,
                        topTracksLong = topTracksLong,
                        recentlyPlayed = recentlyPlayed
                    )
                }
            } catch (e: HttpException) {
                screenState = screenState.copy(isLoading = false, httpStatusCode = e.response()?.code(), errorMessage = e.message(), errorCause = e)
                Log.e("SpotifyDataScreen", "HttpException: , ${e.response()?.code()}, $e, ${e.message}")
            } catch (e: IOException) {
                screenState = screenState.copy(isLoading = false, errorMessage = "Network Error", errorCause = e)
                Log.e("SpotifyDataScreen", "Network Error: , $e, ${e.message}")
            } catch (e: Exception) {
                Log.e("SpotifyDataScreen", "Failed to load data", e)
                screenState = screenState.copy(isLoading = false, errorMessage = e.message, errorCause = e)
            }

        }
    }

    // Initial data fetch
    LaunchedEffect(Unit) {
        fetchData()
    }

    when {
        screenState.isLoading -> {
            LoadingContent()
        }

        screenState.httpStatusCode?.let { isAuthenticationExpired(it)} == true -> {
            AuthenticationExpiredContent(context)
        }

        screenState.errorMessage?.let { isNetworkError(it) } == true -> {
            NetworkErrorContent(onRetry = { fetchData() })
        }

        screenState.errorMessage != null -> {
            ErrorContent(screenState.errorMessage, onRetry = { fetchData() })
        }

        else -> {
            MainPage(screenState)
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