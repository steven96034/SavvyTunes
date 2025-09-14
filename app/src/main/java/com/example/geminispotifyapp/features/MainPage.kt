package com.example.geminispotifyapp.features

import android.content.res.Configuration
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.geminispotifyapp.ui.AppNavHost
import com.example.geminispotifyapp.ui.MAIN_GRAPH_ROUTE
import com.example.geminispotifyapp.ui.theme.SpotifyBlack
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
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
                    Text(dialogMessage)
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("OK") }
            }
        )
    }



//    // 創建 PagerState 來管理底部導航頁面的狀態
//    val pagerState = rememberPagerState(initialPage = 0) {
//        bottomNavItems.size // 底部導航頁面的總數
//    }
//
//    // 當 NavController 的目的地改變時，同步 PagerState
//    LaunchedEffect(navController) {
//        navController.currentBackStackEntryFlow
//            .distinctUntilChanged { old, new ->
//                old.destination.route == new.destination.route
//            }
//            .collect { backStackEntry ->
//                val currentRoute = backStackEntry.destination.route
//                val newPageIndex = bottomNavItems.indexOfFirst { it.route == currentRoute }
//                if (newPageIndex != -1 && pagerState.currentPage != newPageIndex) {
//                    scope.launch {
//                        pagerState.animateScrollToPage(newPageIndex)
//                    }
//                }
//            }
//    }
//
//    // 當 PagerState 的頁面改變時 (例如手勢滑動)，同步 NavController
//    LaunchedEffect(pagerState) {
//        snapshotFlow { pagerState.currentPage }
//            .distinctUntilChanged()
//            .collect { page ->
//                val targetRoute = bottomNavItems[page].route
//                val currentRoute = navController.currentBackStackEntry?.destination?.route
//                if (targetRoute != currentRoute) {
//                    navController.navigate(targetRoute) {
//                        popUpTo(navController.graph.findStartDestination().id) {
//                            saveState = true
//                        }
//                        launchSingleTop = true
//                        restoreState = true
//                    }
//                }
//            }
//    }
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
                navigationIcon = {
                    if (currentDestination?.route == "settings" || currentDestination?.route == "profile" || currentDestination?.route == "aboutThisApp") {
                        IconButton(onClick = { navController.navigate(MAIN_GRAPH_ROUTE) { popUpTo(MAIN_GRAPH_ROUTE) { inclusive = true } } }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        settingsItems.forEach { screen ->
                            val isCurrentDestination = currentDestination?.route == screen.route
                            DropdownMenuItem(
                                text = { Text(screen.label) },
                                leadingIcon = { Icon(screen.icon, contentDescription = screen.label) },
                                onClick = {
                                    if (!isCurrentDestination) {
                                        navController.navigate(screen.route)
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
//        NavHost(navController, startDestination = "main") {
//            composable("main") {
//                MainScreenWithPager(
//                    paddingValues,
//                    pagerState,
//                    selectedScreen
//                ) { item -> viewModel.showItemDetail(item) }
//            }
//            composable("settings") {
//                UserSettingsScreen(paddingValues)
//            }
//            composable("aboutThisApp") {
//                AboutThisAppScreen(paddingValues)
//            }
//        }

//        NavHost(
//            navController = navController,
//            startDestination = Screen.Home.route
//        ) {
//            // HomePage 不參與底部導航的滑動
//            composable(Screen.Home.route) {
//                HomeScreen(paddingValues)
//            }
//            composable(
//                route = Screen.TopArtists.route,
//                enterTransition = { // 從右側進入並淡入
//                    slideIntoContainer(
//                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
//                        animationSpec = tween(500)
//                    ) + fadeIn(animationSpec = tween(500))
//                },
//                exitTransition = { // 向左側退出並淡出
//                    slideOutOfContainer(
//                        towards = AnimatedContentTransitionScope.SlideDirection.End,
//                        animationSpec = tween(500)
//                    ) + fadeOut(animationSpec = tween(500))
//                },
//                popEnterTransition = { // 從左側進入並淡入 (返回時)
//                    slideIntoContainer(
//                        towards = AnimatedContentTransitionScope.SlideDirection.End,
//                        animationSpec = tween(500)
//                    ) + fadeIn(animationSpec = tween(500))
//                },
//                popExitTransition = { // 向右側退出並淡出 (返回時)
//                    slideOutOfContainer(
//                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
//                        animationSpec = tween(500)
//                    ) + fadeOut(animationSpec = tween(500))
//                }
//            ) {
//                TestScreen(navController)
////                TopArtistContent(
////                    data.topArtistsShort,
////                    data.topArtistsMedium,
////                    data.topArtistsLong,
////                    navController,
////                    paddingValues
////                )
//            }
//            composable(Screen.TopTracks.route) {
//                TestTopTracksScreen()
//            }
//            composable(Screen.RecentlyPlayed.route) {
//                TestRecentlyPlayedScreen()
//            }
////            composable(Screen.TopTracks.route) {
////                TopTracksScreen(
////                    navController,
////                    paddingValues
////                )
////            }
////            composable(Screen.RecentlyPlayed.route) {
////                RecentlyPlayedContent(
////                    data.recentlyPlayed,
////                    navController,
////                    paddingValues
////                )
////            }
//        }
        }
        // This box is placed outside the Scaffold to cover the entire screen
//        viewModel.DetailBox (
//            selectedValue = selectedItemForDetail,
//            onDismiss = { viewModel.dismissItemDetail() }
//        ) { item, onDismiss ->
//            when (bottomNavItems[selectedScreen]) {
//                is Screen.TopArtists -> ArtistDetail(item as SpotifyArtist, onDismiss)
//                is Screen.TopTracks -> TrackDetail(item as SpotifyTrack, onDismiss)
//                is Screen.RecentlyPlayed -> TrackHistoryDetail(item as PlayHistoryObject, onDismiss)
//                is Screen.Home -> {
//                    if (item is SpotifyArtist) ArtistDetail(item, onDismiss)
//                    else if (item is SpotifyTrack) TrackDetail(item, onDismiss)
//                }
//                is Screen.FindMusic ->
//            }
//        }
    }
}



@Composable
fun TestFindMusicContent() {
    ContentScreen("This is the Find Music Screen.")
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

/**
 *  Set a modifier for onTap to hide keyboard and clear focus.
 */
fun Modifier.autoCloseKeyboardClearFocus(): Modifier = composed {
    val keyBoardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    pointerInput(this) {
        detectTapGestures(onTap = {
            keyBoardController?.hide()
            focusManager.clearFocus()
        })
    }
}

@Preview
@Composable
fun MainPagePreview() {
    MainPage()
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