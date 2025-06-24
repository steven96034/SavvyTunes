package com.example.geminispotifyapp.features

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.home.HomeScreen
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedScreen
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.TrackHistoryDetail
import com.example.geminispotifyapp.features.userdatadetail.topartists.ArtistDetail
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistsScreen
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksScreen
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TrackDetail
import com.example.geminispotifyapp.ui.theme.SpotifyBlack
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    object Home : Screen("home", Icons.Default.Home, "Home")
    object TopArtists : Screen("topArtists", Icons.Default.PersonPin, "Top Artists")
    object TopTracks : Screen("topTracks", Icons.Default.Favorite, "Top Tracks")
    object RecentlyPlayed : Screen("recentlyPlayed", Icons.Default.AccessTime, "Recently Played")
    object FindMusic : Screen("findMusic", Icons.Default.FindInPage, "Find Music")
}

var bottomNavItems = listOf(
    Screen.Home,
    Screen.TopArtists,
    Screen.TopTracks,
    Screen.RecentlyPlayed,
    Screen.FindMusic
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPage(viewModel: MainViewModel = hiltViewModel()) {
    //val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val selectedTrackForDetail by viewModel.selectedItemForDetail.collectAsStateWithLifecycle()

    // Use WindowSizeClass to determine more kinds of screen size (e.g. Compact, Medium, Expanded), here's only for phone's orientation
//    val configuration = LocalConfiguration.current
//    val bottomBarHeight = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 106.dp else 56.dp
//    val items = remember { listOf(Screen.TopArtists, Screen.TopTracks, Screen.RecentlyPlayed) }

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

    // To fulfill the circle sliding, we set the total pages to Int.MAX_VALUE
    // From a big number in the middle, then user can slide left or right for a long time
    val startPage = Int.MAX_VALUE / 2
    val initialPage = startPage - (startPage % bottomNavItems.size)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    // Back handler logic
    val currentScreenIndex = pagerState.currentPage % bottomNavItems.size
    // Only when the current page is not HomePage (index 0), enable the BackHandler
    BackHandler(enabled = currentScreenIndex != 0) {
        scope.launch {
            // Calculate the pages to scroll to (HomePage)
            pagerState.animateScrollToPage(pagerState.currentPage - currentScreenIndex)
        }
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
    val selectedScreen = pagerState.currentPage % bottomNavItems.size
    Scaffold (
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            //TODO: Replace MyTopAppBar with SmallTopAppBar
            TopAppBar(title = {Text("Music Explorer by Gemini")})
            //MyTopAppBar(navController, scrollBehavior)
        },
        bottomBar = {
//            val navBackStackEntry by navController.currentBackStackEntryAsState()
//            val currentDestination = navBackStackEntry?.destination
            //val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            NavigationBar {
                bottomNavItems.forEachIndexed { index, screen ->
                    // Use the modulo operator (%) to map the "infinite" page index back to the actual page index
                    val selected = selectedScreen == index

                    NavigationBarItem(
                        label = { Text(screen.label) },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        selected = selected,
                        onClick = {
                            // When clicking on a navigation item, calculate the shortest distance to the target page and scroll
                            scope.launch {
                                val currentPosition = pagerState.currentPage
                                val currentOffset = currentPosition % bottomNavItems.size
                                val targetOffset = index
                                val pageDifference = targetOffset - currentOffset
                                // Roll to the nearest corresponding page
                                pagerState.animateScrollToPage(currentPosition + pageDifference)
                            }
                        },
                        alwaysShowLabel = false
                    )
                }
            }


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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            // According to the page index, determine which page to display
            val screenIndex = page % bottomNavItems.size
            when (bottomNavItems[screenIndex]) {
                is Screen.Home -> HomeScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                    onTrackClick = { track -> viewModel.showItemDetail(track) }
                )
                is Screen.TopArtists -> TopArtistsScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) }
                )
                is Screen.TopTracks -> TopTracksScreen(
                    onTrackClick = { track -> viewModel.showItemDetail(track) }
                )
                is Screen.RecentlyPlayed -> RecentlyPlayedScreen(
                    onHistoryClick = { history -> viewModel.showItemDetail(history) }
                )
                is Screen.FindMusic -> TestFindMusicContent()
            }
        }
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
        viewModel.DetailBox (
            selectedValue = selectedTrackForDetail,
            onDismiss = { viewModel.dismissItemDetail() }
        ) { item, onDismiss ->
            when (bottomNavItems[selectedScreen]) {
                is Screen.TopArtists -> ArtistDetail(item as SpotifyArtist, onDismiss)
                is Screen.TopTracks -> TrackDetail(item as SpotifyTrack, onDismiss)
                is Screen.RecentlyPlayed -> TrackHistoryDetail(item as PlayHistoryObject, onDismiss)
                is Screen.Home -> {
                    if (item is SpotifyArtist) ArtistDetail(item, onDismiss)
                    else if (item is SpotifyTrack) TrackDetail(item, onDismiss)
                }
                is Screen.FindMusic -> TODO()
            }
        }
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