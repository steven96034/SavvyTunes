package com.example.geminispotifyapp.features

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
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.findmusic.FindMusicScreen
import com.example.geminispotifyapp.features.findmusic.FindMusicViewModel
import com.example.geminispotifyapp.features.home.HomeScreen
import com.example.geminispotifyapp.features.home.HomeViewModel
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedScreen
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.RecentlyPlayedViewModel
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.TrackHistoryDetail
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.UiPlayHistoryObject
import com.example.geminispotifyapp.features.userdatadetail.topartists.ArtistDetail
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistsScreen
import com.example.geminispotifyapp.features.userdatadetail.topartists.TopArtistsViewModel
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksScreen
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TopTracksViewModel
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TrackDetail
import com.example.geminispotifyapp.init.MAIN_APP_ROUTE
import com.example.geminispotifyapp.init.MainScreen
import com.example.geminispotifyapp.init.bottomNavItems
import kotlinx.coroutines.launch

@Composable
fun MainScreenWithPager(
    backStackEntry: NavBackStackEntry,
    navController: NavHostController,
    viewModel: MainScreenWithPagerViewModel = hiltViewModel()
) {
    val selectedItemForDetail by viewModel.selectedItemForDetail.collectAsStateWithLifecycle()
    val checkMarketIfPlayable by viewModel.checkMarketIfPlayable.collectAsStateWithLifecycle()

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

    LaunchedEffect(selectedScreen) {
        val currentScreen = bottomNavItems[selectedScreen]
        val newTitle = currentScreen.label
        viewModel.uiEventManager.sendEvent(UiEvent.UpdateAppBarTitle(newTitle))
        Log.d("MainScreenWithPager", "Pager page changed to: ${currentScreen.label}, sending title update.")
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
                            is UiPlayHistoryObject -> TrackHistoryDetail(item, checkMarketIfPlayable)
                        }
                        if ((item is SpotifyTrack || item is UiPlayHistoryObject) && currentScreenIndex != 4) { // Not display at home page
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

