package com.example.geminispotifyapp.features.home

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import com.example.geminispotifyapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.TracksAndArtists
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.userdatadetail.topartists.ArtistItem
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TrackItem
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(onArtistClick: (SpotifyArtist) -> Unit, onTrackClick: (SpotifyTrack) -> Unit, viewModel: HomeViewModel = hiltViewModel()) { // Use hiltViewModel() to inject the ViewModel...
    val uiState by viewModel.searchSimilarUiState.collectAsState()
    val trackInput by viewModel.trackInput.collectAsState()
    val artistInput by viewModel.artistInput.collectAsState()
    val suggestedUiState by viewModel.searchDataUiState.collectAsState()


    HomePage(
        uiState,
        suggestedUiState,
        trackInput,
        artistInput,
        onArtistClick,
        onTrackClick,
        { newTrack ->
            viewModel.onTrackInputChange(newTrack)
            viewModel.searchTrack(newTrack) },
        { newArtist -> viewModel.onArtistInputChange(newArtist) },
        { track, artist -> viewModel.searchSimilarTracksAndArtists(track, artist) }
    )
}

@Composable
fun HomePage(
    uiState: SearchUiState,
    suggestedUiState: SearchUiState,
    trackInput: String,
    artistInput: String,
    onArtistClick: (SpotifyArtist) -> Unit,
    onTrackClick: (SpotifyTrack) -> Unit,
    onTrackInputChange: (String) -> Unit,
    onArtistInputChange: (String) -> Unit,
    searchSimilarTracksAndArtists: (String, String) -> Unit
) {
    //val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // TODO: Move to viewmodel
    var selectedSuggestedTrack by remember { mutableStateOf<SpotifyTrack?>(null) }
    var hasSelectedTrackAndInputDoesNotChange by remember { mutableStateOf(false) }

    HomeNavigation()


    //Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(6.dp, 12.dp).padding(paddingValues).verticalScroll(scrollState)) {
    LazyColumn(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp, 0.dp, 6.dp, 12.dp)
            //.padding(paddingValues)
        //.verticalScroll(scrollState)
    ) {
        item {
            Image(
                painterResource(R.drawable.full_logo_green_rgb),
                contentDescription = "Spotify Logo"
            )
        }
        item {
            Spacer(modifier = Modifier.padding(4.dp))
        }
        item {
            Text(
                text = "This demo App takes usage of user data from Spotify, " +
                        "there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(8.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = trackInput,
                    onValueChange = {
                        onTrackInputChange(it)
                        hasSelectedTrackAndInputDoesNotChange = false },
                    label = { Text("Input Song Name") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                OutlinedTextField(
                    value = artistInput,
                    onValueChange = { onArtistInputChange(it) },
                    label = { Text("Input Artist Name") }, //選擇樂團或歌手名稱
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }
        }
//        item {
//            //val suggestedTracks by viewModel.suggestedTracks.collectAsState()
//            var suggestedTracks: List<SpotifyTrack>? = null
//            if (suggestedUiState is SearchUiState.Success)
//                suggestedTracks = suggestedUiState.data.tracks
//            if (suggestedTracks?.isNotEmpty() == true && trackInput.isNotBlank()) {
//                LazyColumn (modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
//                    items(suggestedTracks.size) { index ->
//                        val track = suggestedTracks[index]
//                        TrackSuggestionItem(track = track) { selectedTrack ->
//                            onTrackInputChange(selectedTrack.name)
//                            selectedSuggestedTrack = selectedTrack
//                            // Optionally, you can also fill the artist name if available
//                            // onArtistInputChange(selectedTrack.artists.firstOrNull()?.name ?: "")
//                            // TODO: viewModel.clearSuggestions() // Clear suggestions after selection
//                        }
//                    }
//                }
//            }
//        }
        // val suggestedTracks by viewModel.suggestedTracks.collectAsState()
        var suggestedTracks: List<SpotifyTrack>? = null
        if (suggestedUiState is SearchUiState.Success) {
            suggestedTracks = suggestedUiState.data.tracks
        }

// 檢查 suggestedTracks 是否有內容並且輸入框不是空的
        if (suggestedTracks?.isNotEmpty() == true && trackInput.isNotBlank() && !hasSelectedTrackAndInputDoesNotChange) {
            // 直接將 suggestedTracks 的項目添加到外部 LazyColumn
            items(
                items = suggestedTracks, // 直接傳遞列表
                key = { track -> track.id } // 可選，但建議為每個項目提供唯一的 key 以提高性能
            )
            { track -> // 每個 'track' 就是 suggestedTracks 中的一個元素
                // TrackSuggestionItem 現在是外部 LazyColumn 的一個直接子項目
                // 您可能需要調整 TrackSuggestionItem 的 Modifier，例如移除外部 padding，
                // 因為它現在由外部 LazyColumn 的 item spacing 控制
                TrackSuggestionItem(
                    track = track,
                    onTrackSelected = { selectedTrack -> onTrackInputChange(selectedTrack.name); selectedSuggestedTrack = selectedTrack
                        onArtistInputChange(selectedTrack.artists.firstOrNull()?.name ?: "")
                    },
                    onFlagSet = { hasSelectedTrackAndInputDoesNotChange = it }
                ) {
                    // Optionally, you can also fill the artist name if available
                    //onArtistInputChange(selectedTrack.artists.firstOrNull()?.name ?: "")
                    // TODO: viewModel.clearSuggestions() // Clear suggestions after selection
                }
            }
        }
        item {
            Button(
                onClick = {
                    //selectedSuggestedTrack = null // Clear selected track when searching
                    scope.launch {
                        searchSimilarTracksAndArtists(
                            trackInput,
                            artistInput
                        )
                    }
                    focusManager.clearFocus()
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                if (uiState != SearchUiState.Loading)
                    Text("Search similar tracks and artists!")
                else
                    Text("Loading... (Tap to Cancel)")
            }
        }
        item {
            selectedSuggestedTrack?.let { track ->
                Spacer(Modifier.padding(8.dp))
                Text("Selected Track Details:", style = MaterialTheme.typography.titleMedium)
                TrackItem(index = 0, track = track) { onTrackClick(it) }// Assuming TrackItem can display a single track
            }
        }
        //item {
        when (uiState) {
            is SearchUiState.Loading -> {
                //Text("Loading")
                item {
                    LoadingContent()
                }
            }

            is SearchUiState.Success -> {
//                item {
//                    Text(
//                        "Success",
//                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
//                        textAlign = TextAlign.Center
//                    )
//                }
                val artists = uiState.data.artists
                val tracks = uiState.data.tracks
                item {
                    Spacer(Modifier.padding(8.dp))
                    Text(
                        "Similar Tracks",
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize
                    )
                }
                if (tracks != null) {
                    item {
                        Spacer(Modifier.padding(4.dp))
                        Column {
                            tracks.forEachIndexed { index, track ->
                                TrackItem(index + 1, track) { onTrackClick(it) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                } else {
                    item { Text("No tracks found") }
                }
                //item { HorizontalDivider(modifier = Modifier.padding(8.dp)) }
                item {
                    Spacer(Modifier.padding(8.dp))
                    Text(
                        "Similar Artists",
                        fontSize = MaterialTheme.typography.headlineSmall.fontSize,
                        textAlign = TextAlign.Center
                    )
                }
                if (artists != null) {
                    item {
                        Spacer(Modifier.padding(4.dp))
                        Column {
                            artists.forEachIndexed { index, artist ->
                                ArtistItem (index + 1, artist) { onArtistClick(it) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                } else {
                    item { Text("No artists found") }
                }
                //}
            }

            is SearchUiState.Error -> {
                item {
                    Text(
                        "Error",
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                //Text("Initial")
            }
        }

//            Spacer(modifier = Modifier.height(16.dp))
//
//            Button(
//                onClick = { viewModel.createAndAddTrackToPlaylist(artistInput) },
//                modifier = Modifier.padding(top = 8.dp)
//            ) {
//                Text("在Spotify中訂閱以下播放清單")
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text("相似藝人:", style = MaterialTheme.typography.headlineSmall)
//            similarArtists.forEach { artist ->
//                Text(artist)
//            }
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            Text("熱門歌曲:", style = MaterialTheme.typography.headlineSmall)
//            LazyColumn {
//                items(topTracks) { track ->
//                    TrackItem(track)
//                }
//            }
    }
    //}
    //item {
//    DetailBox(
//        selectedValue = onTrackSelected,
//        onDismiss = { onTrackSelected = null }) { track, onDetailDismiss ->
//        TrackDetail(
//            track = track,
//            onDismiss = onDetailDismiss
//        )
//    }
    //}
}

@Composable
fun TrackSuggestionItem(
    track: SpotifyTrack,
    onTrackSelected: (SpotifyTrack) -> Unit,
    onFlagSet: (Boolean) -> Unit,
    function: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onTrackSelected(track)
                onFlagSet(true)
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // TODO:　Use lower quality image to improve performance
            AsyncImage(
                model = track.album.images.firstOrNull()?.url,
                contentDescription = "Album Art",
                modifier = Modifier.padding(end = 8.dp)
            )
            Column {
                Text(track.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    track.artists.joinToString(separator = ", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun HomeNavigation() {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity

    val backCallback = remember {
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.finish()
            }
        }
    }

    DisposableEffect (lifecycleOwner, backDispatcher) {
        backDispatcher?.addCallback(lifecycleOwner, backCallback)
        onDispose {
            backCallback.remove()
        }
    }
}

@Composable
fun LoadingContent() {
    Box(Modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Preview
@Composable
fun HomePagePreview() {
    val state = SearchUiState.Success(TracksAndArtists(listOf(), listOf()))
    val suggestedState = SearchUiState.Success(TracksAndArtists(listOf(), listOf()))

    fun mockSearchSimilar() {
        // TODO: mock data
    }

    HomePage(state, suggestedState, "", "", {}, {}, {}, {}, { _, _ -> mockSearchSimilar() })
}
