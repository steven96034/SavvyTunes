package com.example.geminispotifyapp.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.geminispotifyapp.features.MainScreenWithPager
import com.example.geminispotifyapp.features.Screen
import com.example.geminispotifyapp.features.bottomNavItems
import com.example.geminispotifyapp.features.settings.AboutThisAppScreen
import com.example.geminispotifyapp.features.settings.UserSettingsScreen

const val MAIN_GRAPH_ROUTE = "main_graph"
@Composable
fun AppNavHost(navController: NavHostController, paddingValues: PaddingValues) {
    NavHost(navController = navController, startDestination = MAIN_GRAPH_ROUTE) {
        // Nested navigation graph to manage Home/TopArtists/TopTracks/RecentlyPlayed/FindMusic ViewModel lifecycle
        navigation(
            startDestination = Screen.Home.route,
            route = MAIN_GRAPH_ROUTE
        ) {
            // The composables in the navigation graph are not displayed directly by the NavHost but by the MainScreenWithPager.

//            composable(Screen.Home.route) { /* ... */ }
//            composable(Screen.TopArtists.route) { /* ... */ }
//            composable(Screen.TopTracks.route) { /* ... */ }
//            composable(Screen.RecentlyPlayed.route) { /* ... */ }
//            composable(Screen.FindMusic.route) { /* ... */ }

            // When route is in bottomNavItems, show MainScreenWithPager.
            bottomNavItems.forEach { screen ->
                composable(screen.route) { backStackEntry ->
                    MainScreenWithPager(
                        paddingValues = paddingValues,
                        backStackEntry = backStackEntry,
                        navController = navController
                    )
                }
            }
        }

        // Independent Settings Pages
        composable("settings") {
            UserSettingsScreen(paddingValues)
        }
        composable("aboutThisApp") {
            AboutThisAppScreen(paddingValues)
        }
    }
}