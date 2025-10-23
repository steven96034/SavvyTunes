package com.example.geminispotifyapp.presentation.features.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.presentation.features.main.home.HomeScreen
import com.example.geminispotifyapp.presentation.features.main.home.HomeViewModel
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicScreen
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicViewModel
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
import com.example.geminispotifyapp.presentation.MAIN_GRAPH_ROUTE
import com.example.geminispotifyapp.presentation.MainScreen
import com.example.geminispotifyapp.presentation.bottomNavItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@Composable
fun MainScreenWithPager(
    navController: NavHostController,
    startPage: String?,
    viewModel: MainScreenWithPagerViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    // Calculate the initial page based on the passed in parameter
    val initialPageIndex = remember(startPage) {
        if (startPage != null) {
            // Find the index of the route corresponding to the parameter in bottomNavItems
            bottomNavItems.indexOfFirst { it.route == startPage }.coerceAtLeast(0)
        } else {
            0 // If there is no parameter, default to the first tab (Home)
        }
    }

    // To fulfill the circle sliding, we set the total pages to Int.MAX_VALUE
    // From a big number in the middle, then user can slide left or right for a long time
    val startPage = Int.MAX_VALUE / 2
    val initialPage = startPage - (startPage % bottomNavItems.size) + initialPageIndex
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { Int.MAX_VALUE }
    )
    val selectedScreenIndex = pagerState.currentPage % bottomNavItems.size

    // Update the pager state when the selected tab (initialPage) changes by navigate by UiEvent.Navigate/UiEvent.ShowSnackbarWithAction
    LaunchedEffect(initialPage) {
        if (selectedScreenIndex != initialPage) {
            pagerState.animateScrollToPage(initialPage)
        }
    }

    // Update the app bar title(dynamicAppBarTitle) when the selected tab changes
    LaunchedEffect(pagerState.currentPage) {
        viewModel.uiEventManager.sendEvent(UiEvent.UpdateAppBarTitle(bottomNavItems[selectedScreenIndex].label))
    }


    // --- ViewModel Scope Setting ---
    val mainGraphBackStackEntry = remember(navBackStackEntry) {
        navController.getBackStackEntry(MAIN_GRAPH_ROUTE)
    }
    val findMusicViewModel: FindMusicViewModel = hiltViewModel(mainGraphBackStackEntry)
    val topArtistsViewModel: TopArtistsViewModel = hiltViewModel(mainGraphBackStackEntry)
    val topTracksViewModel: TopTracksViewModel = hiltViewModel(mainGraphBackStackEntry)
    val recentlyPlayedViewModel: RecentlyPlayedViewModel = hiltViewModel(mainGraphBackStackEntry)
    val homeViewModel: HomeViewModel = hiltViewModel(mainGraphBackStackEntry)

    Scaffold (
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                ,containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {

                bottomNavItems.forEachIndexed { index, screen ->
                    val selected = selectedScreenIndex == index

                    NavigationBarItem(
                        label = { Text(screen.label) },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        selected = selected,
                        onClick = {
                                // Directly handle navigation to the corresponding page by clicking icon here
                                scope.launch {
                                    val currentPosition = pagerState.currentPage
                                    val currentOffset = currentPosition % bottomNavItems.size
                                    val targetOffset = index
                                    val pageDifference = targetOffset - currentOffset
                                    // Scroll to the nearest corresponding page
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
    ) { innerPadding ->
        MainContentWithPager(viewModel, innerPadding, pagerState, scope, findMusicViewModel, topArtistsViewModel, topTracksViewModel, recentlyPlayedViewModel, homeViewModel)
    }
}

@Composable
fun MainContentWithPager(
    viewModel: MainScreenWithPagerViewModel,
    innerPadding: PaddingValues,
    pagerState: PagerState,
    scope: CoroutineScope,
    findMusicViewModel: FindMusicViewModel,
    topArtistsViewModel: TopArtistsViewModel,
    topTracksViewModel: TopTracksViewModel,
    recentlyPlayedViewModel: RecentlyPlayedViewModel,
    homeViewModel: HomeViewModel
) {
    val selectedItemForDetail by viewModel.selectedItemForDetail.collectAsStateWithLifecycle()
    val checkMarketIfPlayable by viewModel.checkMarketIfPlayable.collectAsStateWithLifecycle()


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
            val screenIndex = page % bottomNavItems.size // Calculate the actual index
            val screen = bottomNavItems[screenIndex]

            val isCurrentPage = (pagerState.currentPage % bottomNavItems.size) == screenIndex

            when (screen) {
                is MainScreen.Home -> {
                    HomeScreen(homeViewModel)

                    // Press back button twice to exit the app (when logged-in)
                    val backPressedOnce = remember { mutableStateOf(false) }
                    BackHandler(enabled = isCurrentPage) {
                        if (backPressedOnce.value) {
                            // Exit the app
                            exitProcess(0)
                        } else {
                            backPressedOnce.value = true
                            scope.launch {
                                viewModel.uiEventManager.sendEvent(UiEvent.ShowSnackbar(
                                    message = "Press again to exit"
                                ))
                                // Reset after a delay
                                delay(2000)
                                backPressedOnce.value = false
                            }
                        }
                    }
                }

                else -> {
                    BackHandler(enabled = isCurrentPage) {
                        scope.launch {
                            // Scroll back to Home page (index 0) when on other pages
                            val currentScreenIndex = pagerState.currentPage % bottomNavItems.size
                            pagerState.animateScrollToPage(pagerState.currentPage - currentScreenIndex)
                        }
                    }
                    when (screen) {
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

                        is MainScreen.FindMusic -> FindMusicScreen(
                            onArtistClick = { artist -> viewModel.showItemDetail(artist) },
                            onTrackClick = { track -> viewModel.showItemDetail(track) },
                            viewModel = findMusicViewModel
                        )

                        else -> null
                    }
                }
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
                        if ((item is SpotifyTrack || item is UiPlayHistoryObject) && (pagerState.currentPage % bottomNavItems.size) != 4) { // Do not display on the Find Music page
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(onClick = {
                                viewModel.dismissItemDetail()
                                viewModel.navigateToFindMusicWithTrackAndArtist(item, findMusicViewModel)
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

