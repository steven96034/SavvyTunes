package com.example.geminispotifyapp.page

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.geminispotifyapp.ScreenState
import com.example.geminispotifyapp.ui.theme.SpotifyGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(screenState: ScreenState) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(navController, scrollBehavior)
        },
        content = { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomePage(paddingValues)
                }
                composable("topArtists") {
                    TopArtistContent(
                        screenState.topArtistsShort,
                        screenState.topArtistsMedium,
                        screenState.topArtistsLong,
                        navController,
                        paddingValues
                    )
                }
                composable("topTracks") {
                    TopTrackContent(
                        screenState.topTracksShort,
                        screenState.topTracksMedium,
                        screenState.topTracksLong,
                        navController,
                        paddingValues
                    )
                }
                composable("recentlyPlayed") {
                    RecentlyPlayedContent(
                        screenState.recentlyPlayed,
                        navController,
                        paddingValues
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigation(navController = navController)
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
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
        navigationIcon = navigationIcon ?: {},
        scrollBehavior = scrollBehavior
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

@OptIn(ExperimentalMaterial3Api::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun TopAppBarPreview() {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    MyTopAppBar(navController, scrollBehavior)
}


@Preview
@Composable
fun BottomNavigationPreview() {
    val navController = rememberNavController()
    BottomNavigation(navController = navController)
}