package com.example.geminispotifyapp.init

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.geminispotifyapp.features.MainScreenWithPager
import com.example.geminispotifyapp.features.MainViewModel
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.init.login.LoginPage
import com.example.geminispotifyapp.init.login.LoginViewModel
import com.example.geminispotifyapp.ui.modifiers.autoCloseKeyboardClearFocus
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import com.example.geminispotifyapp.features.settings.AboutThisAppScreen
import com.example.geminispotifyapp.features.settings.ProfileScreen
import com.example.geminispotifyapp.features.settings.UserSettingsScreen

const val SPLASH_ROUTE = "splash_route"
const val LOGIN_ROUTE = "login_route"
const val MAIN_APP_ROUTE = "main_app_route"
sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "Home")
    object TopArtists : Screen("topArtists", Icons.Default.PersonPin, "Top Artists")
    object TopTracks : Screen("topTracks", Icons.Default.Favorite, "Top Tracks")
    object RecentlyPlayed : Screen("recentlyPlayed", Icons.Default.AccessTime, "Recently Played")
    object FindMusic : Screen("findMusic", Icons.Default.FindInPage, "Find Music")
}

sealed class SettingsScreen(val route: String, val icon: ImageVector, val label: String) {
    object Settings : SettingsScreen("settings", Icons.Default.Settings, "Settings")
    object Profile : SettingsScreen("profile", Icons.Default.AccountCircle, "Profile")
    object AboutThisApp : SettingsScreen("aboutThisApp", Icons.Default.Info, "About This App")
}

var bottomNavItems = listOf(
    Screen.Home,
    Screen.TopArtists,
    Screen.TopTracks,
    Screen.RecentlyPlayed,
    Screen.FindMusic
)

val settingsItems = listOf(
    SettingsScreen.Settings,
    SettingsScreen.Profile,
    SettingsScreen.AboutThisApp
)

@Composable
fun RootApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val isAuthenticated by loginViewModel.isAuthenticated.collectAsStateWithLifecycle()
    var initialAuthCheckCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(isAuthenticated, initialAuthCheckCompleted) {
        if (initialAuthCheckCompleted) {
            val targetRoute = if (isAuthenticated) MAIN_APP_ROUTE else LOGIN_ROUTE
            navController.navigate(targetRoute) {
                popUpTo(SPLASH_ROUTE) { inclusive = true }
            }
        }
    }

    LaunchedEffect(loginViewModel.isAuthenticated) {
        loginViewModel.isAuthenticated.collect { authStatus ->
            if (!initialAuthCheckCompleted) {
                initialAuthCheckCompleted = true
            }
        }
    }
    // AppContainer wraps up whole NavHost to ensure all the displayed screens have their features
    AppContainer(rootNavController = navController) { rootPaddingValues -> // rootPaddingValues 來自 AppContainer 的 Scaffold
        NavHost(
            navController = navController,
            startDestination = SPLASH_ROUTE,
            modifier = Modifier.padding(rootPaddingValues)
        ) {
            composable(SPLASH_ROUTE) {
                SplashScreen()
            }

            composable(LOGIN_ROUTE) { backStackEntry ->
                LoginPage(
                    viewModel = loginViewModel,
                )
            }

            // Nested navigation graph for the main app content
            navigation(
                startDestination = Screen.Home.route,
                route = MAIN_APP_ROUTE
            ) {
                // The composables in the navigation graph are not displayed directly by the NavHost but by the MainScreenWithPager.
                // When route is in bottomNavItems, show MainScreenWithPager.
                bottomNavItems.forEach { screen ->
                    composable(screen.route) { backStackEntry ->
                        MainScreenWithPager(
                            backStackEntry = backStackEntry,
                            navController = navController
                        )
                    }
                }
            }

            composable(SettingsScreen.Settings.route) {
                UserSettingsScreen()
            }
            composable(SettingsScreen.Profile.route) {
                ProfileScreen(rootPaddingValues)
            }
            composable(SettingsScreen.AboutThisApp.route) {
                AboutThisAppScreen()
            }
        }
    }
}

// This new Composable will serve as the top-level container for the application, providing Scaffold and event handling.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContainer(
    rootNavController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    val mainViewModel: MainViewModel = hiltViewModel()
    val navBackStackEntry by rootNavController.currentBackStackEntryAsState()
    val currentDestinationRoute = navBackStackEntry?.destination?.route

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val tag = "UiEvent"
    var dialogMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        Log.d("AppContainer_Collector", "LAUNCHED EFFECT STARTED.")
        mainViewModel.uiEventManager.eventFlow.collect { event ->
            Log.d("AppContainer_Collector", ">>>>>> EVENT RECEIVED: $event")
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message, withDismissAction = true, duration = SnackbarDuration.Short)
                    Log.d(tag, "AppContainer_Collector: ${event.message}")
                }
                is UiEvent.Navigate -> {
                    rootNavController.navigate(event.route)
                    Log.d(tag, "AppContainer_Collector: ${event.route}")
                }
                is UiEvent.ShowSnackbarDetail -> {
                    val snackbarActionResult = snackbarHostState.showSnackbar(event.message, "Details", true, SnackbarDuration.Short)
                    Log.d(tag, "AppContainer_Collector: ${event.message}")
                    when (snackbarActionResult) {
                        SnackbarResult.ActionPerformed -> {
                            dialogMessage = event.detail
                            showDialog = true
                            Log.d(tag, "AppContainer_Collector: 'Details' action performed for: ${event.message}")
                        }
                        SnackbarResult.Dismissed -> {
                            Log.d(tag, "AppContainer_Collector: Snackbar dismissed for: ${event.message}")
                        }
                    }
                }
                is UiEvent.ShowSnackbarWithAction -> {
                    if (event.actionLabel == Screen.Home.label) {
                        val snackbarActionResult = snackbarHostState.showSnackbar(
                            event.message,
                            "See Result",
                            true,
                            SnackbarDuration.Long
                        )
                        if (snackbarActionResult == SnackbarResult.ActionPerformed) {
                            rootNavController.navigate(Screen.Home.route) {
                                popUpTo(MAIN_APP_ROUTE) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                            Log.d(
                                tag,
                                "AppContainer_Collector: Navigating to Home triggered by Snackbar action."
                            )
                        }
                    }
                }
                is UiEvent.Unauthorized -> {
                    rootNavController.navigate(LOGIN_ROUTE) {
                        popUpTo(MAIN_APP_ROUTE) { inclusive = true }
                    }
                    snackbarHostState.showSnackbar(event.message)
                    Log.d(tag, "AppContainer_Collector: ${event.message}")
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Details") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LazyColumn {
                        item {
                            Text(dialogMessage)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .autoCloseKeyboardClearFocus()
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Music Explorer by Gemini") },
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        if (currentDestinationRoute in settingsItems.map { it.route }
                            ) {
                            IconButton(onClick = { rootNavController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                            }
                        }
                    },
                    actions = {
                        val isLoginPage = currentDestinationRoute == LOGIN_ROUTE
                        if (!isLoginPage) {
                            IconButton(onClick = { showMenu = !showMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                settingsItems.forEach { screen ->
                                    val isCurrentDestination = currentDestinationRoute == screen.route
                                    DropdownMenuItem(
                                        text = { Text(screen.label) },
                                        leadingIcon = { Icon(screen.icon, contentDescription = screen.label) },
                                        onClick = {
                                            if (!isCurrentDestination) {
                                                rootNavController.navigate(screen.route)
                                            }
                                            showMenu = false
                                        },
                                        colors = MenuDefaults.itemColors(
                                            textColor = if (isCurrentDestination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            leadingIconColor = if (isCurrentDestination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = if (isCurrentDestination) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) else Modifier
                                    )
                                }
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { snackbarData ->
                        Snackbar(
                            snackbarData = snackbarData,
                            modifier = Modifier.padding(16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = Color.White,
                            actionColor = SpotifyGreen,
                            dismissActionContentColor = Color.LightGray
                        )
                    }
                )
            }
        ) { paddingValues ->
            content(paddingValues)
        }
    }
}

@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}