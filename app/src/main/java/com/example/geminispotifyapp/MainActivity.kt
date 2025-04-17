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
import androidx.activity.OnBackPressedCallback
import com.example.geminispotifyapp.ui.theme.GeminiSpotifyAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.app.Activity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geminispotifyapp.auth.AuthManager
import com.example.geminispotifyapp.data.SharedData.GET_ITEM_NUM
import com.example.geminispotifyapp.page.HomePage
import com.example.geminispotifyapp.page.RecentlyPlayedContent
import com.example.geminispotifyapp.page.TopArtistContent
import com.example.geminispotifyapp.page.TopTrackContent
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import kotlinx.coroutines.async
import retrofit2.HttpException


class MainActivity : ComponentActivity() {

    private lateinit var spotifyDataManager: SpotifyDataManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 SpotifyDataManager
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
            Log.d("Auth", "User is authenticated, entering data screen")
        } else {
            Log.d("Auth", "User is not authenticated, entering login screen")
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
    fun fetchData() {
        scope.launch {
            screenState = screenState.copy(isLoading = true, httpStatusCode = null ,errorMessage = null, errorCause = null)
            try {
                Log.d("SpotifyDataScreen", "Fetching data...")
//                spotifyDataManager.refreshTokenDataIfNeeded()

                val topArtistsDeferredShort = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopArtists(timeRange = "short_term", limit = GET_ITEM_NUM)
                }
                val topArtistDeferredMedium = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopArtists(timeRange = "medium_term", limit = GET_ITEM_NUM)
                }
                val topArtistsDeferredLong = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopArtists(timeRange = "long_term", limit = GET_ITEM_NUM)
                }

                val topTracksDeferredShort = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopTracks(timeRange = "short_term", limit = GET_ITEM_NUM)
                }
                val topTracksDeferredMedium = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopTracks(timeRange = "medium_term", limit = GET_ITEM_NUM)
                }
                val topTracksDeferredLong = async(Dispatchers.IO) {
                    spotifyDataManager.getUserTopTracks(timeRange = "long_term", limit = GET_ITEM_NUM)
                }

                val recentlyPlayedDeferred = async(Dispatchers.IO) {
                    spotifyDataManager.getRecentlyPlayedTracks(limit = GET_ITEM_NUM)
                }

                screenState = screenState.copy(
                    isLoading = false,
                    topArtistsShort = topArtistsDeferredShort.await().items,
                    topArtistsMedium = topArtistDeferredMedium.await().items,
                    topArtistsLong = topArtistsDeferredLong.await().items,
                    topTracksShort = topTracksDeferredShort.await().items,
                    topTracksMedium = topTracksDeferredMedium.await().items,
                    topTracksLong = topTracksDeferredLong.await().items,
                    recentlyPlayed = recentlyPlayedDeferred.await().items
                )
            } catch (e: HttpException) {
                screenState = screenState.copy(isLoading = false, httpStatusCode = e.response()?.code(), errorMessage = e.message(), errorCause = e)
            }
            catch (e: Exception) {
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
            Box(Modifier
                .fillMaxSize()
                .padding(4.dp)) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        isAuthenticationExpired(screenState.httpStatusCode) -> {
            Box(Modifier
                .fillMaxSize()
                .padding(4.dp)) {
                AuthenticationExpiredContent(context)
            }
        }

        screenState.errorMessage != null -> {
            Box(Modifier
                .fillMaxSize()
                .padding(4.dp)) {
                ErrorContent(screenState.errorMessage, onRetry = { fetchData() })
            }
        }

        else -> {
            val navController = rememberNavController() // 初始化 NavController
            Scaffold (
                topBar = {
                    MyTopAppBar(navController = navController)
                },
                content = { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(padding)
                    ) {
                        composable("home") {
                            HomePage() // 首頁內容
                        }
                        composable("topArtists") {
                            TopArtistContent(screenState.topArtistsShort, screenState.topArtistsMedium, screenState.topArtistsLong, navController)
                        }
                        composable("topTracks") {
                            TopTrackContent(screenState.topTracksShort, screenState.topTracksMedium, screenState.topTracksLong, navController)
                        }
                        composable("recentlyPlayed") {
                            RecentlyPlayedContent(screenState.recentlyPlayed, navController)
                        }
                    }
                },
                bottomBar = {
                    BottomNavigation(navController = navController)
                }
            )
        }
    }
}


/**
 * Handles back navigation in the app. When the current destination is not "home",
 * it navigates back to the "home" destination. If the current destination is "home"
 * and the user presses back, it finishes the activity.
 */
@Composable
fun HandleBackToHome(navController: NavController) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navController.navigateToHome()
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

fun NavController.navigateToHome() {
    this.navigate("home") {
        popUpTo(this@navigateToHome.graph.findStartDestination().id){
            inclusive = false // keep home page in stack
        }
        launchSingleTop = true // avoid multiple copies of the same destination
        restoreState = true // restore state when reselecting a previously selected item
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val title = when (currentRoute) {
        "topArtists" -> "Top Artists"
        "topTracks" -> "Top Tracks"
        "recentlyPlayed" -> "Recently Played"
        else -> "My Spotify Data"
    }
    val navigationIcon: (@Composable () -> Unit)? = if (currentRoute != "home") {
        {
            IconButton(onClick = { navController.navigate("home") }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }
    } else {
        null
    }
    TopAppBar(
        title = { Text(title) },
        navigationIcon = navigationIcon ?: {}
    )
}


@Composable
fun BottomNavigation(navController: NavController) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Row (
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(
            onClick = { navController.navigate("topArtists"); Log.d("BottomNavigation", "$currentRoute -> next") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
//            if (isSystemInDarkTheme()) {
//                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
//            } else {
//                ButtonDefaults.buttonColors()
//            }

        ) {
            Icon(Icons.Default.AccountCircle, "topArtists", tint = SpotifyGreen)
        }
        Button(
            onClick = { navController.navigate("topTracks"); Log.d("BottomNavigation", "$currentRoute  -> next") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)

        ) {
            Icon(Icons.Default.Favorite, "topTracks", tint = SpotifyGreen)
        }
        Button(
            onClick = { navController.navigate("recentlyPlayed"); Log.d("BottomNavigation", "$currentRoute  -> next") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Icon(Icons.Default.FavoriteBorder, "recentlyPlayed", tint = SpotifyGreen)
        }
    }
}


@Preview
@Composable
fun BottomNavigationPreview() {
    val navController = rememberNavController()
    BottomNavigation(navController = navController)
}


@Composable
fun DropDownMenuTemplate(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    options: List<String>
) {
    Box (contentAlignment = Alignment.CenterEnd) {
        IconButton(onClick = { onExpandChange(true) }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Period Selection"
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) }
        ) {
            options.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    modifier = Modifier.padding(2.dp),
                    onClick = {
                        onExpandChange(false)
                        onValueChange(index)
                    },
                    trailingIcon = {
                        if (selectedValue == index) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected"
                            )
                        }
                    }
                )
            }
        }
    }
}