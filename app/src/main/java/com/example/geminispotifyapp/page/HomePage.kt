package com.example.geminispotifyapp.page

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import com.example.geminispotifyapp.R

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geminispotifyapp.HomePageViewModel
import com.example.geminispotifyapp.HomePageViewModelFactory
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyTrack
import kotlinx.coroutines.launch



@Composable
fun HomePage(spotifyRepository: SpotifyRepository, paddingValues: PaddingValues) {
    val scrollState = rememberScrollState()

    var onTrackSelected by remember { mutableStateOf<SpotifyTrack?>(null)}

    val viewModel: HomePageViewModel = viewModel(factory = HomePageViewModelFactory(spotifyRepository))
    val uiState by viewModel.searchSimilarUiState.collectAsState()
    val trackInput by viewModel.trackInput.collectAsState()
    val artistInput by viewModel.artistInput.collectAsState()

    val scope = rememberCoroutineScope()

    HomeNavigation()

    //Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(6.dp, 12.dp).padding(paddingValues).verticalScroll(scrollState)) {
        LazyColumn (verticalArrangement = Arrangement.Top,
            modifier = Modifier.fillMaxSize()
                .padding(6.dp, 12.dp)
                .padding(paddingValues)
                //.verticalScroll(scrollState)
        ) {
            item {
                Image(
                    painterResource(R.drawable.full_logo_green_rgb),
                    contentDescription = "Spotify Logo"
                )
                Spacer(modifier = Modifier.padding(4.dp))
                Text(
                    text = "This demo App takes usage of user data from Spotify, " +
                            "there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(4.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(8.dp))

                Row {
                    OutlinedTextField(
                        value = trackInput,
                        onValueChange = { viewModel.onTrackInputChange(it) },
                        label = { Text("Input Song Name") },
                        modifier = Modifier.fillMaxWidth(0.4f).padding(2.dp)
                    )
                    OutlinedTextField(
                        value = artistInput,
                        onValueChange = { viewModel.onArtistInputChange(it) },
                        label = { Text("Artist Name") }, //選擇樂團或歌手名稱
                        modifier = Modifier.fillMaxWidth(0.4f).padding(2.dp)
                    )
                }
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.searchSimilarTracksAndArtists(
                                trackInput,
                                artistInput
                            )
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
                ) {
                    Text("Search similar tracks and artists!")
                }
                when (uiState) {
                    is SearchUiState.Loading -> {
                        //Text("Loading")
                        LoadingContent()
                    }

                    is SearchUiState.Success -> {
                        Text(
                            "Success",
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            textAlign = TextAlign.Center
                        )
                        val artists = (uiState as SearchUiState.Success).data.artists
                        val tracks = (uiState as SearchUiState.Success).data.tracks
                        Text(
                            "相似歌曲:",
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        )
                        if (tracks != null) {
                            Column {
                                tracks.forEachIndexed { index, track ->
//                                    TrackItem(index + 1, track) { onTrackSelected = it }

                                    Row {
                                        Text("$index. " + track.name, modifier = Modifier.padding(2.dp))
                                        Column {
                                            Text(
                                                "Artists: " + track.artists.joinToString(", ") { it.name },
                                                modifier = Modifier.padding(2.dp)
                                            )
                                            Text(
                                                "Popularity: " + track.popularity.toString(),
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                    }
                                    //HorizontalDivider(modifier = Modifier.padding(2.dp))
                                }
                            }
                        } else {
                            Text("No tracks found")
                        }
                        HorizontalDivider(modifier = Modifier.padding(8.dp))
                        Text(
                            "相似藝人:",
                            fontSize = MaterialTheme.typography.headlineSmall.fontSize
                        )
                        if (artists != null) {
                            Column {
                                artists.forEach { artist ->
                                    Row {
                                        Column (modifier = Modifier.padding(2.dp)) {
                                            Text(artist.name, modifier = Modifier.padding(2.dp))
                                            Text(
                                                "Pop: " + artist.popularity.toString(),
                                                modifier = Modifier.padding(2.dp)
                                            )
                                        }
                                        Text(
                                            "Genres:\n" + artist.genres.joinToString(", "),
                                            modifier = Modifier.padding(2.dp)
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(2.dp))
                                }
                            }
                        } else {
                            Text("No artists found")
                        }
                    }

                    is SearchUiState.Error -> {
                        Text(
                            "Error",
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            textAlign = TextAlign.Center
                        )
                    }

                    else -> {
                        //Text("Initial")
                    }
                }
                DetailBox(selectedValue = onTrackSelected, onDismiss = { onTrackSelected = null }) { track, onDetailDismiss ->
                    TrackDetail(
                        track = track,
                        onDismiss = onDetailDismiss
                    )
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