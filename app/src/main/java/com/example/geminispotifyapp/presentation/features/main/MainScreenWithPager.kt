package com.example.geminispotifyapp.presentation.features.main

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicScreen
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicViewModel
import com.example.geminispotifyapp.presentation.features.main.home.HomeScreen
import com.example.geminispotifyapp.presentation.features.main.home.HomeViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.RecentlyPlayedScreen
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.RecentlyPlayedViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.TrackHistoryDetail
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.UiPlayHistoryObject
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.topartists.ArtistDetail
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.topartists.TopArtistsScreen
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.topartists.TopArtistsViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.toptracks.TopTracksScreen
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.toptracks.TopTracksViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.toptracks.TrackDetail
import com.example.geminispotifyapp.presentation.MAIN_APP_ROUTE
import com.example.geminispotifyapp.presentation.MainScreen
import com.example.geminispotifyapp.presentation.bottomNavItems
import kotlinx.coroutines.launch

@Composable
fun MainScreenWithPager(
    backStackEntry: NavBackStackEntry,
    navController: NavHostController,
    viewModel: MainScreenWithPagerViewModel = hiltViewModel()
) {
    val selectedItemForDetail by viewModel.selectedItemForDetail.collectAsStateWithLifecycle()
    val checkMarketIfPlayable by viewModel.checkMarketIfPlayable.collectAsStateWithLifecycle()

    val currentEntryRoute = backStackEntry.destination.route
    // Based on `currentEntryRoute` to determine the initial page index
    val initialPageOffset = remember(currentEntryRoute) {
        bottomNavItems.indexOfFirst { it.route == currentEntryRoute }
            .coerceAtLeast(0) // make sure the index is non-negative
    }

    // To fulfill the circle sliding, we set the total pages to Int.MAX_VALUE
    // From a big number in the middle, then user can slide left or right for a long time
    val startPage = Int.MAX_VALUE / 2
    val initialPage = startPage - (startPage % bottomNavItems.size)  + initialPageOffset
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )

    // Monitor changes to the current destination of the rootNavController
    // Then the Pager will scroll to the corresponding page as outer place navigating to the route in MainScreen
    val currentRootNavEntry by navController.currentBackStackEntryAsState()
    val currentRootDestinationRoute = currentRootNavEntry?.destination?.route

    val scope = rememberCoroutineScope()

    // Calculate pages of scroll, considering infinite scroll

    LaunchedEffect(currentRootDestinationRoute) {
        // Only scroll the Pager when the rootNavController's route is inconsistent with the Pager's currently displayed page
        val targetScreen = bottomNavItems.find { it.route == currentRootDestinationRoute }
        if (targetScreen != null) {
            val targetPageIndex = bottomNavItems.indexOf(targetScreen)
            val currentVisiblePageIndex = pagerState.currentPage % bottomNavItems.size

            if (targetPageIndex != currentVisiblePageIndex) {
                // If the Pager is currently scrolling, do not scroll automatically to avoid interfering with user gestures
                // This is one of the keys to preventing flickering
                if (!pagerState.isScrollInProgress) {
                    val pagesToScroll = targetPageIndex - currentVisiblePageIndex
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + pagesToScroll)
                    }
                    Log.d("MainScreenWithPager", "Root navigator triggers scroll to the: ${targetScreen.label} (Index $targetPageIndex)。")
                } else {
                    Log.d("MainScreenWithPager", "Root navigator detects a route change, but Pager is scrolling, so we don't need to scroll automatically.")
                }
            }
        }
    }

    // Back handler logic
    val selectedScreenIndex = pagerState.currentPage % bottomNavItems.size
    // Only when the current page is not HomePage (index 0), enable the BackHandler
    BackHandler(enabled = selectedScreenIndex != 0) {
        scope.launch {
            // Calculate the pages to scroll to (HomePage)
            pagerState.animateScrollToPage(pagerState.currentPage - selectedScreenIndex)
        }
    }

    // When the Pager page changes (either manually or programmatically), update the AppBar title and synchronize the root navigator
    LaunchedEffect(pagerState.currentPage) {
        val newSelectedScreenIndex = pagerState.currentPage % bottomNavItems.size
        val currentScreen = bottomNavItems[newSelectedScreenIndex]
        val newTitle = currentScreen.label
        viewModel.uiEventManager.sendEvent(UiEvent.UpdateAppBarTitle(newTitle))
        Log.d("MainScreenWithPager", "Pager page changes to: ${currentScreen.label} (index $newSelectedScreenIndex, update title to $newTitle).")

        // Sync the root navigator to the current Pager page route
        val currentRouteInNavController = navController.currentBackStackEntry?.destination?.route
        val targetRoute = currentScreen.route
        // Make sure that when navigating to the main application graph, the navigation stack is not cleared

        if (currentRouteInNavController != targetRoute) {
            // If the Pager is being scrolled by a user gesture and is still in progress,
            // do not trigger NavController.navigate yet. Let the Pager state stabilize first.
            if (!pagerState.isScrollInProgress) {
                navController.navigate(targetRoute) {
                    popUpTo(MAIN_APP_ROUTE) { inclusive = false }
                    launchSingleTop = true
                }
                Log.d("MainScreenWithPager", "Pager scroll to: ${currentScreen.label}, sync root navigator to $targetRoute.")
            } else {
                Log.d("MainScreenWithPager", "Pager detects page is changing, but Pager is scrolling, so we don't need to sync root navigator.")
            }
        }
    }


    // --- ViewModel Scope Setting ---
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(MAIN_APP_ROUTE)
    }

    val homeViewModel: HomeViewModel = hiltViewModel(parentEntry)
    val topArtistsViewModel: TopArtistsViewModel = hiltViewModel(parentEntry)
    val topTracksViewModel: TopTracksViewModel = hiltViewModel(parentEntry)
    val recentlyPlayedViewModel: RecentlyPlayedViewModel = hiltViewModel(parentEntry)
    val findMusicViewModel: FindMusicViewModel = hiltViewModel(parentEntry)

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
        ) { page ->
            // According to the page index, determine which page to display
            val screenIndex = page % bottomNavItems.size
            when (bottomNavItems[screenIndex]) {
                is MainScreen.Home -> HomeScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                    onTrackClick = { track -> viewModel.showItemDetail(track) },
                    viewModel = homeViewModel
                )

                is MainScreen.TopArtists -> TopArtistsScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                    viewModel = topArtistsViewModel
                )

                is MainScreen.TopTracks -> TopTracksScreen(
                    onTrackClick = { track -> viewModel.showItemDetail(track) },
                    viewModel = topTracksViewModel
                )

                is MainScreen.RecentlyPlayed -> RecentlyPlayedScreen(
                    onHistoryClick = { history -> viewModel.showItemDetail(history) },
                    viewModel = recentlyPlayedViewModel
                )

                is MainScreen.FindMusic -> FindMusicScreen(findMusicViewModel)
            }
        }
        NavigationBar(
            modifier = Modifier
                .align(Alignment.BottomCenter) // Align to bottom center
                .fillMaxWidth()
                // Add bottom padding to avoid overlap with system gesture navigation bar
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            bottomNavItems.forEachIndexed { index, screen ->
                // Use the modulo operator (%) to map the "infinite" page index back to the actual page index
                //val selected = selectedScreen == index

                // Use `selectedScreenIndex` to determine the selection state
                val selected = selectedScreenIndex == index

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
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }
    }
    if (selectedItemForDetail != null) {
        Dialog(
            onDismissRequest = { viewModel.dismissItemDetail() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val scrollState = rememberScrollState()
            val item = selectedItemForDetail

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.75f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(top = 16.dp, bottom = 64.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                        , horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (item) {
                            is SpotifyArtist -> ArtistDetail(item)
                            is SpotifyTrack -> TrackDetail(item, checkMarketIfPlayable)
                            is UiPlayHistoryObject -> TrackHistoryDetail(
                                item,
                                checkMarketIfPlayable
                            )
                        }
                        if ((item is SpotifyTrack || item is UiPlayHistoryObject) && selectedScreenIndex != 4) { // Not display at home page
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = {
                                viewModel.dismissItemDetail()
                                viewModel.navigateToHomeWithTrackAndArtist(item, homeViewModel)
                            }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                                Text("Find similar tracks and artists!")
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Button(
                        onClick = { viewModel.dismissItemDetail() },
                        modifier = Modifier.padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(text = "Close")
                    }
                }
            }
        }
    }
}

