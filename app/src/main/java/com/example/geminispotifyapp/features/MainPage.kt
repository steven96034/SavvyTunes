package com.example.geminispotifyapp.features

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.geminispotifyapp.ui.AppNavHost
import com.example.geminispotifyapp.ui.MAIN_GRAPH_ROUTE
import com.example.geminispotifyapp.ui.modifiers.autoCloseKeyboardClearFocus
import com.example.geminispotifyapp.ui.theme.SpotifyGreen

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

sealed class MoreScreen(val route: String, val icon: ImageVector, val label: String) {
    object LoginPage : MoreScreen("login", Icons.Default.AccountCircle, "Login")
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }

    // Use WindowSizeClass to determine more kinds of screen size (e.g. Compact, Medium, Expanded), here's only for phone's orientation
//    val configuration = LocalConfiguration.current
//    val bottomBarHeight = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 106.dp else 56.dp
//    val items = remember { listOf(Screen.TopArtists, Screen.TopTracks, Screen.RecentlyPlayed) }

    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val tag = "UiEvent"
    var dialogMessage by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        Log.d("MainPager_Collector", "LAUNCHED EFFECT STARTED.")
        viewModel.uiEventManager.eventFlow.collect { event ->
            Log.d("MainPager_Collector", ">>>>>> EVENT RECEIVED: $event")
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(message = event.message, withDismissAction = true, duration = SnackbarDuration.Short)
                    Log.d(tag, "MainPager_Collector: ${event.message}")
                }
                is UiEvent.Navigate -> {
                    navController.navigate(event.route)
                    Log.d(tag, "MainPager_Collector: ${event.route}")
                }
                is UiEvent.ShowSnackbarDetail -> {
                    val snackbarActionResult = snackbarHostState.showSnackbar(event.message, "Details", true, SnackbarDuration.Short)
                    Log.d(tag, "MainPager_Collector: ${event.message}")
                    when (snackbarActionResult) {
                        SnackbarResult.ActionPerformed -> {
                            // User clicked the "Details" button
                            dialogMessage = event.detail
                            showDialog = true
                            Log.d(tag, "MainPager_Collector: 'Details' action performed for: ${event.message}")

                        }
                        SnackbarResult.Dismissed -> {
                            Log.d(tag, "MainPager_Collector: Snackbar dismissed for: ${event.message}")
                        }
                    }
                }
                is UiEvent.Unauthorized -> {
                    navController.navigate(event.navigationRoute)
                    snackbarHostState.showSnackbar(event.message)
                    Log.d(tag, "MainPager_Collector: ${event.message}")
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

    MainScreen(
        navController = navController,
        currentDestination = currentDestination,
        currentDestinationRoute = currentDestination?.route,
        scrollBehavior = scrollBehavior,
        snackbarHostState = snackbarHostState,
        showMenu = showMenu,
        onShowMenuChange = { showMenu = it },
        onNavigateToMainGraph = { navController.navigate(MAIN_GRAPH_ROUTE) { popUpTo(MAIN_GRAPH_ROUTE) { inclusive = true } } },
        onNavigateToScreen = { route -> navController.navigate(route) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    currentDestination: NavDestination?,
    currentDestinationRoute: String?,
    scrollBehavior: TopAppBarScrollBehavior,
    snackbarHostState: SnackbarHostState,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
    onNavigateToMainGraph: () -> Unit,
    onNavigateToScreen: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold (
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .autoCloseKeyboardClearFocus()
            .windowInsetsPadding(
                WindowInsets.navigationBars
            ),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Music Explorer by Gemini") },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    if (currentDestinationRoute == "settings" || currentDestinationRoute == "profile" || currentDestinationRoute == "aboutThisApp") {
                        IconButton(onClick = onNavigateToMainGraph) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onShowMenuChange(!showMenu) }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { onShowMenuChange(false) }
                    ) {
                        settingsItems.forEach { screen ->
                            val isCurrentDestination = currentDestination?.route == screen.route
                            DropdownMenuItem(
                                text = { Text(screen.label) },
                                leadingIcon = { Icon(screen.icon, contentDescription = screen.label) },
                                onClick = {
                                    if (!isCurrentDestination) {
                                        navController.navigate(screen.route)
                                        onNavigateToScreen(screen.route)
                                    }
                                    onShowMenuChange(false)
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
            )
        },
        bottomBar = {
//            val navBackStackEntry by navController.currentBackStackEntryAsState()
//            val currentDestination = navBackStackEntry?.destination
            //val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

//            NavigationBar (
//                modifier = Modifier.fillMaxWidth(),
//                containerColor = Color.Transparent
//            ) {
//                bottomNavItems.forEachIndexed { index, screen ->
//                    // Use the modulo operator (%) to map the "infinite" page index back to the actual page index
//                    val selected = selectedScreen == index
//
//                    NavigationBarItem(
//                        label = { Text(screen.label) },
//                        icon = { Icon(screen.icon, contentDescription = screen.label) },
//                        selected = selected,
//                        onClick = {
//                            // When clicking on a navigation item, calculate the shortest distance to the target page and scroll
//                            scope.launch {
//                                val currentPosition = pagerState.currentPage
//                                val currentOffset = currentPosition % bottomNavItems.size
//                                val targetOffset = index
//                                val pageDifference = targetOffset - currentOffset
//                                // Roll to the nearest corresponding page
//                                pagerState.animateScrollToPage(currentPosition + pageDifference)
//                            }
//                        },
//                        alwaysShowLabel = false,
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = MaterialTheme.colorScheme.primary, // 您想要的選中圖示顏色
//                            selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,   // 您想要的選中文字顏色
//                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, // 您想要的未選中圖示顏色
//                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, // 您想要的未選中文字顏色
//                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant // (可選) 選中項的指示器背景色
//                        )
//                    )
//                }
//            }


            //if (currentRoute in bottomNavItems.map { it.route }) {
//                NavigationBar {
//                    bottomNavItems.forEach { screen ->
//                        val selected = navController.currentBackStackEntryAsState().value?.destination?.hierarchy?.any { it.route == screen.route } == true
//                        NavigationBarItem(
//                            icon = { Icon(screen.icon, contentDescription = screen.label) },
//                            label = { Text(screen.label) },
//                            selected = selected,
//                            onClick = {
//                                navController.navigate(screen.route) {
//                                    popUpTo(navController.graph.findStartDestination().id) {
//                                        saveState = true
//                                    }
//                                    launchSingleTop = true
//                                    restoreState = true
//                                }
//                            }
//                        )
//                    }
//                }
            //}
//            //BottomNavigation(navController = navController)
//            NavigationBar (modifier = Modifier.height(bottomBarHeight)){
////                val items = listOf(
////                    Triple("topArtists", "Top Artists", Icons.Default.AccountCircle),
////                    Triple("topTracks", "Top Tracks", Icons.Default.Favorite),
////                    Triple("recentlyPlayed", "Recently Played", Icons.Default.FavoriteBorder)
////                )
//                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
//
//
//                items.forEach { screen ->
//                    val isSelected = currentRoute == screen.route
//
//                    NavigationBarItem(
//                        icon = { Icon(screen.icon, screen.label) }, // 傳遞計算好的 itemColor
//                        label = { Text(screen.label) },
//                        selected = isSelected,
//                        onClick = {
//                            if (currentRoute != screen.route) {
//                                navController.navigate(screen.label) {
//                                    popUpTo(navController.graph.findStartDestination().id) {
//                                        saveState = true
//                                    }
//                                    launchSingleTop = true
//                                    restoreState = true
//                                }
//                            }
//                        },
//                        colors = NavigationBarItemDefaults.colors(
//                            selectedIconColor = SpotifyBlack,
//                            selectedTextColor = SpotifyWhite.copy(alpha = 0.7f),
//                            unselectedIconColor = SpotifyWhite.copy(alpha = 0.6f),
//                            unselectedTextColor = SpotifyWhite,
//                            indicatorColor = SpotifyGreen.copy(alpha = 0.8f)
//                        ),
//                        alwaysShowLabel = false
//                    )
//                }
//            }
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
        AppNavHost(
            navController = navController,
            paddingValues = paddingValues
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    MainScreen(
        navController = navController,
        currentDestination = navController.currentDestination,
        currentDestinationRoute = navController.currentDestination?.route,
        scrollBehavior = scrollBehavior,
        snackbarHostState = snackbarHostState,
        showMenu = showMenu,
        onShowMenuChange = { showMenu = it },
        onNavigateToMainGraph = { navController.navigate(MAIN_GRAPH_ROUTE) { popUpTo(MAIN_GRAPH_ROUTE) { inclusive = true } } },
        onNavigateToScreen = { route -> navController.navigate(route) }
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MainScreenDarkPreview() {
    MainScreenPreview()
}

@Composable
fun ContentScreen(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}