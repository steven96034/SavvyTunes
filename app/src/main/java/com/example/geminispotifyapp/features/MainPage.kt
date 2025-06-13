package com.example.geminispotifyapp.features

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.geminispotifyapp.UserData
import com.example.geminispotifyapp.features.home.HomeScreen
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedContent
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistContent
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTrackContent
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksScreen
import com.example.geminispotifyapp.ui.theme.SpotifyBlack
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(data: UserData, viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(snackbarHostState) {
        viewModel.snackbarEvent.collect { event ->
            scope.launch {
                //val messageText: String
                val result: SnackbarResult
                when (event) {
                    is SnackbarMessage.TextMessage -> {
                        //snackbarHostState.showSnackbar(event.message)
                        result = snackbarHostState.showSnackbar(
                            message = event.message,
                            withDismissAction = true,
                            duration = event.duration
                        )
                        //messageText = event.message
                    }
                    // TODO: Make more user friendly error message...
                    is SnackbarMessage.ExceptionMessage -> {
                        //snackbarHostState.showSnackbar(event.exception.localizedMessage ?: "Some Error Happened...")
                        result = snackbarHostState.showSnackbar(
                            message = event.exception.localizedMessage ?: "Some Error Happened...",
                            withDismissAction = true,
                            duration = event.duration
                        )
                        //event.exception.localizedMessage ?: "Some Error Happened..."
                    }

                    is SnackbarMessage.ActionMessage -> {
                        result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            withDismissAction = true,
                            duration = event.duration
                        )
                        //event.message
                    }
                    is SnackbarMessage.ResourceMessage -> {
                        result = snackbarHostState.showSnackbar(
                            message = context.getString(event.resourceId),
                            withDismissAction = true,
                            duration = event.duration
                        )
                        //context.getString(event.resourceId)
                    }
                }
                Log.d("Snackbar", "$event")
                if (result == SnackbarResult.ActionPerformed && event is SnackbarMessage.ActionMessage) event.onAction.invoke()
            }
        }
    }

    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(navController, scrollBehavior)
        },
        bottomBar = {
            BottomNavigation(navController = navController)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home"
        ) {
            composable("home") {
                HomeScreen(paddingValues)
            }
            composable("topArtists") {
                TopArtistContent(
                    data.topArtistsShort,
                    data.topArtistsMedium,
                    data.topArtistsLong,
                    navController,
                    paddingValues
                )
            }
            composable("topTracks") {
                TopTracksScreen(
                    navController,
                    paddingValues
                )
            }
            composable("recentlyPlayed") {
                RecentlyPlayedContent(
                    data.recentlyPlayed,
                    navController,
                    paddingValues
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTopAppBar(navController: NavController, scrollBehavior: TopAppBarScrollBehavior) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val title = when (currentRoute) {
        "topArtists" -> "Top Artists"
        "topTracks" -> "Top Tracks"
        "recentlyPlayed" -> "Recently Played"
        else -> "Music Explorer by Gemini"
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
        title = {
            if (title != "Music Explorer by Gemini") Text(title)
            else Text("Music Explorer by Gemini", color = SpotifyGreen, style = MaterialTheme.typography.headlineLarge)
                },
        navigationIcon = navigationIcon ?: {},
        scrollBehavior = scrollBehavior
    )
}
data class CustomBottomNavItem(
    val route: String,
    val icon: @Composable (tint: Color) -> Unit, // Let the caller decide how to display the icon
    val label: String
)

@Composable
fun BottomNavigation(navController: NavController) {
    val items = listOf(
        CustomBottomNavItem("topArtists", { Icon(Icons.Default.AccountCircle, "topArtists", tint = it) }, "Top Artists"),
        CustomBottomNavItem("topTracks", { Icon(Icons.Default.Favorite, "topTracks", tint = it) }, "Top Tracks"),
        CustomBottomNavItem("recentlyPlayed", { Icon(Icons.Default.FavoriteBorder, "recentlyPlayed", tint = it) }, "Recently Played")
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route


    Row (
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val itemColor = if (isSelected) SpotifyBlack else SpotifyGreen // Change color based on selection
            Column(
                modifier = Modifier
                    .background(if (isSelected) SpotifyGreen.copy(alpha = 0.3f) else SpotifyBlack)
                    .clickable {
                        // Navigation logic should be placed here
                        if (currentRoute != item.route) { // Prevent navigating to the same destination
                            navController.navigate(item.route, navOptions {
                                // Pop up to the start destination of the graph to avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            })
                        }
                    }
                    .weight(1f)
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item.icon(itemColor) // Pass the itemColor to the icon composable
                Text(text = item.label, color = itemColor)
            }
        }

//        Button(
//            onClick = { navController.navigate("topArtists"); Log.d("BottomNavigation", "$currentRoute -> next") },
//            modifier = Modifier.weight(1f),
//            shape = RoundedCornerShape(0.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
////            if (isSystemInDarkTheme()) {
////                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
////            } else {
////                ButtonDefaults.buttonColors()
////            }
//
//        ) {
//            Icon(Icons.Default.AccountCircle, "topArtists", tint = SpotifyGreen)
//        }
//        Button(
//            onClick = { navController.navigate("topTracks"); Log.d("BottomNavigation", "$currentRoute  -> next") },
//            modifier = Modifier.weight(1f),
//            shape = RoundedCornerShape(0.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
//
//        ) {
//            Icon(Icons.Default.Favorite, "topTracks", tint = SpotifyGreen)
//        }
//        Button(
//            onClick = { navController.navigate("recentlyPlayed"); Log.d("BottomNavigation", "$currentRoute  -> next") },
//            modifier = Modifier.weight(1f),
//            shape = RoundedCornerShape(0.dp),
//            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background)
//        ) {
//            Icon(Icons.Default.FavoriteBorder, "recentlyPlayed", tint = SpotifyGreen)
//        }
    }
}

@Preview
@Composable
fun MainPagePreview() {
    MainPage(UserData())
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