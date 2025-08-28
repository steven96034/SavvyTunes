package com.example.geminispotifyapp.features

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.home.HomeScreen
import com.example.geminispotifyapp.features.home.HomeViewModel
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedScreen
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedViewModel
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.TrackHistoryDetail
import com.example.geminispotifyapp.features.userdatadetail.topartists.ArtistDetail
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistsScreen
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistsViewModel
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksScreen
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksViewModel
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TrackDetail
import com.example.geminispotifyapp.ui.MAIN_GRAPH_ROUTE
import kotlinx.coroutines.launch

@Composable
fun MainScreenWithPager(
    paddingValues: PaddingValues,
    backStackEntry: NavBackStackEntry,
    navController: NavHostController,
    viewModel: MainScreenWithPagerViewModel = hiltViewModel()
) {
    val selectedItemForDetail by viewModel.selectedItemForDetail.collectAsStateWithLifecycle()
    //val pagerState = rememberPagerState()

//    // --- 同步邏輯 (保持不變) ---
//    val currentRoute = backStackEntry.destination.route
//    LaunchedEffect(currentRoute) {
//        val pageIndex = screens.indexOfFirst { it.route == currentRoute }
//        if (pageIndex != -1 && pagerState.currentPage != pageIndex) {
//            pagerState.animateScrollToPage(pageIndex)
//        }
//    }
//    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
//        if (!pagerState.isScrollInProgress) {
//            val newRoute = screens[pagerState.currentPage].route
//            if (currentRoute != newRoute) {
//                navController.navigate(newRoute) {
//                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
//                    launchSingleTop = true
//                    restoreState = true
//                }
//            }
//        }
//    }

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
    val scope = rememberCoroutineScope()
    // Only when the current page is not HomePage (index 0), enable the BackHandler
    BackHandler(enabled = currentScreenIndex != 0) {
        scope.launch {
            // Calculate the pages to scroll to (HomePage)
            pagerState.animateScrollToPage(pagerState.currentPage - currentScreenIndex)
        }
    }
    val selectedScreen = pagerState.currentPage % bottomNavItems.size


    // --- ViewModel Scope Setting ---
    val parentEntry = remember(backStackEntry) {
        navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
    }

    val homeViewModel: HomeViewModel = hiltViewModel(parentEntry)
    val topArtistsViewModel: TopArtistsViewModel = hiltViewModel(parentEntry)
    val topTracksViewModel: TopTracksViewModel = hiltViewModel(parentEntry)
    val recentlyPlayedViewModel: RecentlyPlayedViewModel = hiltViewModel(parentEntry)
    // TODO: Add other viewModels here

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
            //.padding(paddingValues)
        ) { page ->
            // According to the page index, determine which page to display
            val screenIndex = page % bottomNavItems.size
            when (bottomNavItems[screenIndex]) {
                is Screen.Home -> HomeScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                    onTrackClick = { track -> viewModel.showItemDetail(track) },
                    viewModel = homeViewModel
                )

                is Screen.TopArtists -> TopArtistsScreen(
                    onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                    viewModel = topArtistsViewModel
                )

                is Screen.TopTracks -> TopTracksScreen(
                    onTrackClick = { track -> viewModel.showItemDetail(track) },
                    viewModel = topTracksViewModel
                )

                is Screen.RecentlyPlayed -> RecentlyPlayedScreen(
                    onHistoryClick = { history -> viewModel.showItemDetail(history) },
                    viewModel = recentlyPlayedViewModel
                )

                is Screen.FindMusic -> TestFindMusicContent()
            }
        }
        NavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter) // Align to bottom center
                .fillMaxWidth()
                // Add bottom padding to avoid overlap with system gesture navigation bar
                .windowInsetsPadding(WindowInsets.navigationBars),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
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
    viewModel.DetailBox (
        selectedValue = selectedItemForDetail,
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

