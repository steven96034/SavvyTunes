package com.example.geminispotifyapp.presentation

import android.R
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.geminispotifyapp.presentation.features.main.MainScreenWithPager
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.presentation.features.login.LoginPage
import com.example.geminispotifyapp.presentation.features.login.LoginViewModel
import com.example.geminispotifyapp.presentation.ui.modifiers.autoCloseKeyboardClearFocus
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyGreen
import com.example.geminispotifyapp.presentation.features.settings.aboutthisapp.AboutThisAppScreen
import com.example.geminispotifyapp.presentation.features.settings.profile.ProfileScreen
import com.example.geminispotifyapp.presentation.features.settings.usersettings.UserSettingsScreen
import com.example.geminispotifyapp.presentation.features.welcome.WelcomeScreen
import com.example.geminispotifyapp.presentation.features.welcome.WelcomeViewModel
import kotlinx.coroutines.launch

const val SPLASH_ROUTE = "splash_route"
const val LOGIN_ROUTE = "login_route"
const val WELCOME_ROUTE = "welcome_route"
// For the main navigation graph (Pager and Settings)
const val MAIN_GRAPH_ROUTE = "main_graph"
// For only Pager layouts
const val MAIN_APP_ROUTE = "main_app_route"
const val START_PAGE_KEY_OF_MAIN_APP = "startPage"
const val MAIN_APP_ROUTE_WITH_PARAM = "$MAIN_APP_ROUTE/{$START_PAGE_KEY_OF_MAIN_APP}"



interface Screen {
    val route: String
    val icon: ImageVector
    val label: String
}

sealed class MainScreen(override val route: String, override val icon: ImageVector, override val label: String): Screen {
    object Home : MainScreen("home", Icons.Default.Home, "Home")
    object TopArtists : MainScreen("topArtists", Icons.Default.PersonPin, "Top Artists")
    object TopTracks : MainScreen("topTracks", Icons.Default.Favorite, "Top Tracks")
    object RecentlyPlayed : MainScreen("recentlyPlayed", Icons.Default.AccessTime, "Recently Played")
    object FindMusic : MainScreen("findMusic", Icons.Default.FindInPage, "Find Music")
}

sealed class SettingsScreen(override val route: String, override val icon: ImageVector, override val label: String): Screen {
    object Settings : SettingsScreen("settings", Icons.Default.Settings, "Settings")
    object Profile : SettingsScreen("profile", Icons.Default.AccountCircle, "Profile")
    object AboutThisApp : SettingsScreen("aboutThisApp", Icons.Default.Info, "About This App")
}

var bottomNavItems = listOf(
    MainScreen.Home,
    MainScreen.TopArtists,
    MainScreen.TopTracks,
    MainScreen.RecentlyPlayed,
    MainScreen.FindMusic
)

val settingsItems = listOf(
    SettingsScreen.Settings,
    SettingsScreen.Profile,
    SettingsScreen.AboutThisApp
)

val allAppScreens = bottomNavItems + settingsItems

// Create a map from route to label
val screenLabelsByRoute: Map<String, String> = allAppScreens.associate {
    it.route to it.label
}


@Composable
fun RootApp() {
    val navController = rememberNavController()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val welcomeViewModel: WelcomeViewModel = hiltViewModel() // Correctly inject WelcomeViewModel
    val isAuthenticated by loginViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val isWelcomeFlowCompleted by welcomeViewModel.isWelcomeFlowCompletedFlow.collectAsStateWithLifecycle(initialValue = false) // Collect from WelcomeViewModel
    var initialAuthCheckCompleted by remember { mutableStateOf(false) }

    LaunchedEffect(isAuthenticated, initialAuthCheckCompleted, isWelcomeFlowCompleted) {
        if (initialAuthCheckCompleted) {
            val targetRoute = if (isAuthenticated) {
                if (isWelcomeFlowCompleted) {
                    MAIN_APP_ROUTE_WITH_PARAM // Already completed welcome flow, go to main
                } else {
                    WELCOME_ROUTE // Authenticated, but welcome flow not completed
                }
            } else {
                LOGIN_ROUTE
            }

            // Only navigate if the current destination is different, to avoid unnecessary re-navigations
            // For MAIN_APP_ROUTE_WITH_PARAM, the currentDestination.route might be different due to the parameter.
            val currentRouteBase = navController.currentDestination?.route?.substringBefore('/')
            val isCurrentRouteMainAppWithParam = currentRouteBase == MAIN_APP_ROUTE && targetRoute == MAIN_APP_ROUTE_WITH_PARAM

            if (navController.currentDestination?.route != targetRoute && !isCurrentRouteMainAppWithParam) { // The second condition ensures that if we are already on the main app route with a different start page, we don't block navigation
                navController.navigate(targetRoute) {
                    popUpTo(SPLASH_ROUTE) { inclusive = true }
                    anim {
                        enter = R.anim.slide_in_left
                        exit = R.anim.slide_out_right
                        popEnter = R.anim.slide_in_left
                        popExit = R.anim.slide_out_right
                    }
                }
            }
        }
    }

    LaunchedEffect(loginViewModel.isAuthenticated) {
        loginViewModel.isAuthenticated.collect {
            if (!initialAuthCheckCompleted) {
                initialAuthCheckCompleted = true
            }
        }
    }
    // AppContainer wraps up whole NavHost to ensure all the displayed screens have their features
    AppContainer(rootNavController = navController) { rootPaddingValues ->
        NavHost(
            navController = navController,
            startDestination = SPLASH_ROUTE,
            modifier = Modifier.padding(rootPaddingValues)
        ) {
            composable(SPLASH_ROUTE) {
                SplashScreen()
            }

            composable(LOGIN_ROUTE) {
                LoginPage(
                    viewModel = loginViewModel,
                )
            }

            composable(WELCOME_ROUTE) {
                WelcomeScreen(
                    navController = navController,
                    viewModel = welcomeViewModel
                )
            }

            // Key modification: Wrap all main pages in a navigation block with a route
            navigation(
                startDestination = MAIN_APP_ROUTE, // The start page of this graph is MainScreen
                route = MAIN_GRAPH_ROUTE // Name the entire graph
            ) {
                // MainScreen registration remains unchanged
                composable(
                    route = MAIN_APP_ROUTE_WITH_PARAM,
                    arguments = listOf(
                        navArgument(START_PAGE_KEY_OF_MAIN_APP) {
                            type = NavType.StringType // Parameter type
                            nullable = true // Set as optional, so navigating directly to MAIN_APP_ROUTE won't cause an error
                        }
                    )
                ) { backStackEntry ->
                    // We need navController to find the graph's backStackEntry
                    val startPage = backStackEntry.arguments?.getString(START_PAGE_KEY_OF_MAIN_APP)
                    MainScreenWithPager(
                        navController = navController, // Pass in the navController
                        startPage = startPage
                    )
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val tag = "UiEvent"
    var dialogMessage by remember { mutableStateOf("") }

    var dynamicAppBarTitle by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        mainViewModel.uiEventManager.eventFlow.collect { event ->
            scope.launch {
                // Launch a new coroutine for each flow-in event, so we don't block the UI thread by handling events
                // Snackbar would not be interfered by new added snackbar event due to the inner design of SnackbarHostState (line-up inside the SnackbarHost)
                Log.d(tag, ">>>>>> EVENT RECEIVED: $event")
                when (event) {
                    is UiEvent.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )
                        Log.d(tag, "Show snackbar event message: ${event.message}")
                    }

                    is UiEvent.Navigate -> {
                        // Get the instant data from the Single Source of Truth, which is the rootNavController
                        val routeBeforeNavigation =
                            rootNavController.currentDestination?.route
                        if (routeBeforeNavigation != event.route) {
                            rootNavController.navigate(event.route) {
                                popUpTo(rootNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        dynamicAppBarTitle = screenLabelsByRoute[event.route]
                        Log.d(tag, "Navigate to event Route: ${event.route}, $dynamicAppBarTitle")
                    }

                    is UiEvent.ShowSnackbarDetail -> {
                        val snackbarActionResult = snackbarHostState.showSnackbar(
                            event.message,
                            "Details",
                            true,
                            SnackbarDuration.Short
                        )
                        Log.d(tag, "Show snackbar detail event message: ${event.message}")
                        when (snackbarActionResult) {
                            SnackbarResult.ActionPerformed -> {
                                dialogMessage = event.detail
                                showDialog = true
                                Log.d(
                                    tag,
                                    "Show snackbar detail: 'Details' action performed for: ${event.message}"
                                )
                            }

                            SnackbarResult.Dismissed -> {
                                Log.d(
                                    tag,
                                    "Show snackbar detail: Snackbar dismissed for: ${event.message}"
                                )
                            }
                        }
                    }

                    is UiEvent.ShowSnackbarWithAction -> {
                        val targetRoute = when (event.actionLabel) {
                            MainScreen.Home.label -> MainScreen.Home.route
                            MainScreen.FindMusic.label -> MainScreen.FindMusic.route
                            else -> null
                        }
                        if (targetRoute != null) {
                            val snackbarActionResult = snackbarHostState.showSnackbar(
                                event.message,
                                "See Result",
                                true,
                                SnackbarDuration.Long
                            )
                            if (snackbarActionResult == SnackbarResult.ActionPerformed) {
                                val routeWithParam = "$MAIN_APP_ROUTE/$targetRoute"

                                // Get the instant data from the Single Source of Truth, which is the rootNavController
                                val routeBeforeNavigation =
                                    rootNavController.currentDestination?.route
                                if (routeBeforeNavigation != routeWithParam) {
                                    rootNavController.navigate(routeWithParam) {
                                        popUpTo(rootNavController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                        // If current route is not in MAIN_APP_ROUTE, animate the navigation
                                        if (routeBeforeNavigation != null && !routeBeforeNavigation.startsWith(
                                                MAIN_APP_ROUTE
                                            )
                                        ) {
                                            Log.d(
                                                tag,
                                                "Animation starts because route is '${routeBeforeNavigation}'.",
                                            )
                                            anim {
                                                enter = R.anim.slide_in_left
                                                exit = R.anim.slide_out_right
                                                popEnter = R.anim.slide_in_left
                                                popExit = R.anim.slide_out_right
                                            }
                                        } else {
                                            Log.d(
                                                tag,
                                                "Animation doesn't start and route is '${routeBeforeNavigation}'.",
                                            )
                                        }
                                    }
                                }
                                Log.d(
                                    tag,
                                    "Show snackbar with action: Navigating to Home triggered by Snackbar action."
                                )
                            }
                        }
                    }

                    is UiEvent.Unauthorized -> {
                        rootNavController.navigate(LOGIN_ROUTE) {
                            popUpTo(rootNavController.graph.findStartDestination().id) {
                                inclusive = true
                            }
                        }
                        snackbarHostState.showSnackbar(event.message)
                        Log.d(tag, "Unauthorized event message: ${event.message}")
                    }

                    is UiEvent.UpdateAppBarTitle -> {
                        dynamicAppBarTitle = event.title
                        Log.d(
                            tag,
                            "AppContainer_Collector: Updated app bar title to: ${event.title}"
                        )
                    }
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
                if (dynamicAppBarTitle != MainScreen.Home.label && currentDestinationRoute != WELCOME_ROUTE) { // Do not show app bar for Welcome screen
                    TopAppBar(
                        title = {
                            val title = dynamicAppBarTitle
                                ?: screenLabelsByRoute[currentDestinationRoute]
                                ?: "Savvy Tunes"

                            Log.d(
                                "Main Scaffold",
                                "currentDestinationRoute: $currentDestinationRoute, Calculated title: $title"
                            )
                            Text(
                                text = title,
                                fontFamily = FontFamily.SansSerif,
                                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        scrollBehavior = scrollBehavior,
                        navigationIcon = {
                            if (currentDestinationRoute in settingsItems.map { it.route } ) {
                                IconButton(onClick = { rootNavController.popBackStack() }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "back"
                                    )
                                }
                            }
                        },
                        actions = {
                            val isLoginPage = currentDestinationRoute == LOGIN_ROUTE

                            if (!isLoginPage) {
                                IconButton(onClick = { showMenu = !showMenu }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = "More options"
                                    )
                                }
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    settingsItems.forEach { screen ->
                                        val isCurrentDestination =
                                            currentDestinationRoute == screen.route
                                        DropdownMenuItem(
                                            text = { Text(screen.label) },
                                            leadingIcon = {
                                                Icon(
                                                    screen.icon,
                                                    contentDescription = screen.label
                                                )
                                            },
                                            onClick = {
                                                rootNavController.navigate(screen.route) {
                                                    launchSingleTop = true
                                                }
                                                dynamicAppBarTitle = null
                                                showMenu = false
                                            },
                                            colors = MenuDefaults.itemColors(
                                                textColor = if (isCurrentDestination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                leadingIconColor = if (isCurrentDestination) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            modifier = if (isCurrentDestination) Modifier.background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(
                                                    alpha = 0.5f
                                                )
                                            ) else Modifier
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
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
