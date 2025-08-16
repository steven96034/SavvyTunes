package com.example.geminispotifyapp.features.home

import android.app.Activity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import com.example.geminispotifyapp.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyDataList
import com.example.geminispotifyapp.data.SimplifiedTrack
import com.example.geminispotifyapp.data.SpotifyAlbum
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyImage
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.data.TrackInformation
import com.example.geminispotifyapp.features.userdatadetail.topartists.ArtistItem
import com.example.geminispotifyapp.features.userdatadetail.toptracks.TrackItem
import com.example.geminispotifyapp.ui.theme.SpotifyGreen
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onArtistClick: (SpotifyArtist) -> Unit, 
    onTrackClick: (SpotifyTrack) -> Unit, 
    viewModel: HomeViewModel = hiltViewModel()
) { 
    val similarUiState by viewModel.searchSimilarUiState.collectAsState()
    val trackInput by viewModel.trackInput.collectAsState()
    val artistInput by viewModel.artistInput.collectAsState()
    val dataInput by viewModel.dataInput.collectAsState()

    val suggestedUiState by viewModel.searchDataUiState.collectAsState()
    val selectedSuggestedTrack by viewModel.selectedSuggestedTrack.collectAsState()
    val selectedSuggestedArtist by viewModel.selectedSuggestedArtist.collectAsState()
    val hasSelectedTrackAndInputDoesNotChange by viewModel.hasSelectedTrackAndInputDoesNotChange.collectAsState()
    val hasSelectedArtistAndInputDoesNotChange by viewModel.hasSelectedArtistAndInputDoesNotChange.collectAsState()
    val hasSelectedDataAndInputDoesNotChange by viewModel.hasSelectedDataAndInputDoesNotChange.collectAsState()
    val hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange by viewModel.hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange.collectAsState()

    val searchByIdUiState by viewModel.searchByIdUiState.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    HomePage(
        similarUiState,
        suggestedUiState,
        searchByIdUiState,
        trackInput,
        artistInput,
        dataInput,
        onArtistClick,
        onTrackClick,
        { newTrack ->
            viewModel.onTrackInputChange(newTrack)
            viewModel.searchTrack(newTrack, HomeViewModel.Type.TRACK, HomeViewModel.Type.TRACK) },
        { newArtist ->
            viewModel.onArtistInputChange(newArtist)
            viewModel.searchTrack(newArtist, HomeViewModel.Type.ARTIST, HomeViewModel.Type.TRACK) }, //TODO: more search for artist
        { newData ->
            viewModel.onDataInputChange(newData)
            viewModel.searchTrack(newData, HomeViewModel.Type.ALLMENTIONED, HomeViewModel.Type.ALLMENTIONED)},
        selectedSuggestedTrack,
        selectedSuggestedArtist,
        { track -> viewModel.onSelectedSuggestedTrackChange(track) },
        { artist -> viewModel.onSelectedSuggestedArtistChange(artist) },
        hasSelectedTrackAndInputDoesNotChange,
        { set -> viewModel.onHasSelectedTrackAndInputDoesNotChangeSet(set) },
        hasSelectedArtistAndInputDoesNotChange,
        { set -> viewModel.onHasSelectedArtistAndInputDoesNotChangeSet(set) },
        hasSelectedDataAndInputDoesNotChange,
        { set -> viewModel.onHasSelectedDataAndInputDoesNotChangeSet(set) },
        hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange,
        { newValue -> viewModel.setHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange(newValue) },
        { track, artist -> viewModel.searchSimilarTracksAndArtists(track, artist) },
        { artistId -> viewModel.getTopTracksOfArtist(artistId) },
        selectedAlbum,
        { album -> viewModel.onSetSelectedAlbum(album) },
        { albumId -> viewModel.getAlbumTracks(albumId) },
        { trackId -> viewModel.getTrackAndSelectedTrack(trackId) }
    )
}

@Composable
fun HomePage(
    uiState: SearchUiState,
    suggestedUiState: SearchUiState,
    searchByIdUiState: SearchUiState,
    trackInput: String,
    artistInput: String,
    dataInput: String,
    onArtistClick: (SpotifyArtist) -> Unit,
    onTrackClick: (SpotifyTrack) -> Unit,
    onTrackInputChange: (String) -> Unit,
    onArtistInputChange: (String) -> Unit,
    onDataInputChange: (String) -> Unit,
    selectedSuggestedTrack: SpotifyTrack?,
    selectedSuggestedArtist: SpotifyArtist?,
    onSelectedSuggestedTrackChange: (SpotifyTrack?) -> Unit,
    onSelectedSuggestedArtistChange: (SpotifyArtist?) -> Unit,
    hasSelectedTrackAndInputDoesNotChange: Boolean,
    onHasSelectedTrackAndInputDoesNotChangeSet: (Boolean) -> Unit,
    hasSelectedArtistAndInputDoesNotChange: Boolean,
    onHasSelectedArtistAndInputDoesNotChangeSet: (Boolean) -> Unit,
    hasSelectedDataAndInputDoesNotChange: Boolean,
    onHasSelectedDataAndInputDoesNotChangeSet: (Boolean) -> Unit,
    // Add new parameters for the state from ViewModel
    hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange: Boolean,
    onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet: (Boolean) -> Unit,
    searchSimilarTracksAndArtists: (String, String) -> Unit,
    getTopTracksOfArtist: (String) -> Unit,
    selectedAlbum: SpotifyAlbum?,
    onSetSelectedAlbum: (SpotifyAlbum?) -> Unit,
    getAlbumTracks: (String) -> Unit,
    getTrackAndSelectedTrack: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    HomeNavigation()

    val textStyleWithShadow = MaterialTheme.typography.headlineSmall.copy(
        shadow = Shadow(
            color = Color.LightGray.copy(alpha = 0.5f),
            blurRadius = 20f
        )
    )

    LazyColumn(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp, 0.dp, 6.dp, 12.dp)
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
                text = "This demo App takes usage of user data from Spotify,\n" +
                        "there may be some places that haven't satisfied Spotify Design Guidelines or Spotify Developer Terms!",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(4.dp)
            )
        }
        item {
            HorizontalDivider(modifier = Modifier.padding(8.dp))
        }
        item {
            OutlinedTextField(
                value = dataInput,
                onValueChange = {
                    onDataInputChange(it)
                    onHasSelectedDataAndInputDoesNotChangeSet(false)
                    onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false)
                },
                trailingIcon = {
                    if (dataInput.isNotEmpty()) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = "Clear text",
                            modifier = Modifier.clickable { onDataInputChange("") }
                        )
                    }
                },
                label = { Text("Input Any Name (of artist, album or track)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = trackInput,
                    onValueChange = {
                        onTrackInputChange(it)
                        onHasSelectedTrackAndInputDoesNotChangeSet(false)
                        onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false) // Use the new function
                    },
                    trailingIcon = {
                                   if (trackInput.isNotEmpty()) {
                                       Icon(
                                           Icons.Filled.Clear,
                                           contentDescription = "Clear text",
                                           modifier = Modifier.clickable { onTrackInputChange("") }
                                       )
                                   }
                    },
                    label = { Text("Input Song Name") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )

                OutlinedTextField(
                    value = artistInput,
                    onValueChange = {
                        onArtistInputChange(it)
                        onHasSelectedArtistAndInputDoesNotChangeSet(false)
                        onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false) // Use the new function
                    },
                    trailingIcon = {
                        if (artistInput.isNotEmpty()) {
                            Icon(
                                Icons.Filled.Clear,
                                contentDescription = "Clear text",
                                modifier = Modifier.clickable { onArtistInputChange("") }
                            )
                        }
                    },
                    label = { Text("Input Artist Name") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
            }
        }
        var suggestedData: SpotifyDataList?
        var suggestedTracks: List<SpotifyTrack>? = null
        var suggestedArtists: List<SpotifyArtist>? = null
        var suggestedAlbums: List<SpotifyAlbum>? = null
        if (suggestedUiState is SearchUiState.Success) {
            suggestedData = suggestedUiState.data
            suggestedTracks = suggestedData.tracks
            suggestedArtists = suggestedData.artists
            suggestedAlbums = suggestedData.albums
        }

        var suggestedTrackById: List<TrackInformation>? = null
        if (searchByIdUiState is SearchUiState.Success) {
            suggestedTrackById = searchByIdUiState.data.trackInformation
        }

        if (suggestedTracks != null &&
            ((trackInput.isNotBlank() && !hasSelectedTrackAndInputDoesNotChange) ||
                    (artistInput.isNotBlank() && !hasSelectedArtistAndInputDoesNotChange) ||
                    (dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange))
        ) {
            if (dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange) {
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = "Track Suggestions",
                            style = textStyleWithShadow,
                            fontFamily = FontFamily.Monospace,
                            color = SpotifyGreen
                        )
                    }

                }
            }
            items(
                items = suggestedTracks,
                key = { track -> track.id }
            ) { track ->
                TrackSuggestionItem(
                    track = track,
                    onTrackSelected = { selectedTrack ->
                        onTrackInputChange(selectedTrack.name)
                        onSelectedSuggestedTrackChange(selectedTrack)
                        onArtistInputChange(selectedTrack.artists.firstOrNull()?.name ?: "")
                        onHasSelectedTrackAndInputDoesNotChangeSet(true)
                        onHasSelectedArtistAndInputDoesNotChangeSet(true)
                        onHasSelectedDataAndInputDoesNotChangeSet(true)
                        focusManager.clearFocus()
                    }
                )
            }
        }
        if (suggestedArtists != null && dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange) {
            item {
                Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "Artist Suggestions",
                        style = textStyleWithShadow,
                        fontFamily = FontFamily.Monospace,
                        color = SpotifyGreen
                    )
                }
            }
            items(
                items = suggestedArtists,
                key = { artist -> artist.id }
            ) {
                ArtistSuggestionItem(
                    artist = it,
                    onArtistSelected = { selectedArtist ->
                        onArtistInputChange(selectedArtist.name)
//                        onSelectedSuggestedArtistChange(selectedArtist)
//                        //onTrackInputChange(selectedArtist.name)
                        onHasSelectedTrackAndInputDoesNotChangeSet(true)
                        onHasSelectedArtistAndInputDoesNotChangeSet(true)
                        onHasSelectedDataAndInputDoesNotChangeSet(true)
                        onArtistInputChange(selectedArtist.name)
                        getTopTracksOfArtist(selectedArtist.id)
                        focusManager.clearFocus()
                    }
                )
            }
        }
        if (suggestedAlbums != null && dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange) {
            item {
                Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                Row (
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Album Suggestions",
                        style = textStyleWithShadow,
                        fontFamily = FontFamily.Monospace,
                        color = SpotifyGreen
                    )
                }
            }
            items(
                items = suggestedAlbums,
                key = { album -> album.id }
            ) {
                AlbumSuggestionItem(
                    album = it,
                    onAlbumSelected = { selectedAlbum ->
                        onDataInputChange(selectedAlbum.name)
                        onHasSelectedTrackAndInputDoesNotChangeSet(true)
                        onHasSelectedArtistAndInputDoesNotChangeSet(true)
                        onHasSelectedDataAndInputDoesNotChangeSet(true)
                        onSetSelectedAlbum(selectedAlbum)
                        getAlbumTracks(selectedAlbum.id)
                        focusManager.clearFocus()
                    }
                )
            }
        }

        if (suggestedTrackById != null && !hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange &&
            (hasSelectedTrackAndInputDoesNotChange && hasSelectedArtistAndInputDoesNotChange && hasSelectedDataAndInputDoesNotChange)) {
            item {
                Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "Track Suggestions",
                        style = textStyleWithShadow,
                        fontFamily = FontFamily.Monospace,
                        color = SpotifyGreen
                    )
                }
            }
            items(
                items = suggestedTrackById,
                key = { trackInfo -> trackInfo.id }
            ) { trackInfo ->
                if (trackInfo is SpotifyTrack) { // Artist
                    TrackSuggestionItem(
                        track = trackInfo,
                        onTrackSelected = { selectedTrack ->
                            onTrackInputChange(selectedTrack.name)
                            onSelectedSuggestedTrackChange(selectedTrack)
                            onArtistInputChange(selectedTrack.artists.joinToString(", ") { it.name })
//                            onHasSelectedTrackAndInputDoesNotChangeSet(true)
//                            onHasSelectedArtistAndInputDoesNotChangeSet(true)
//                            onHasSelectedDataAndInputDoesNotChangeSet(true)
                            onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(true) // Use the new function
                            suggestedTrackById = null
                            focusManager.clearFocus()
                        }
                    )
                }
                else if (trackInfo is SimplifiedTrack) { // Album
                    SimplifiedTrackSuggestionItem(
                        selectedAlbum = selectedAlbum,
                        track = trackInfo,
                        onTrackSelected = { selectedSimplifiedTrack ->
                            onTrackInputChange(selectedSimplifiedTrack.name)
                            getTrackAndSelectedTrack(selectedSimplifiedTrack.id)

                            onArtistInputChange(selectedSimplifiedTrack.artists.joinToString(", ") { it.name })
//                            onHasSelectedTrackAndInputDoesNotChangeSet(true)
//                            onHasSelectedArtistAndInputDoesNotChangeSet(true)
//                            onHasSelectedDataAndInputDoesNotChangeSet(true)
                           onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(true)
                            suggestedTrackById = null
                            focusManager.clearFocus()
                        }
                    )
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
                Text("Selected Track", style = MaterialTheme.typography.headlineSmall)
                TrackItem(index = 0, track = track) { onTrackClick(it) }
            }
        }
        when (uiState) {
            is SearchUiState.Loading -> {
                item {
                    LoadingContent()
                }
            }

            is SearchUiState.Success -> {
                val artists = uiState.data.artists
                val tracks = uiState.data.tracks
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Text(
                        "Similar Tracks",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
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
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Text(
                        "Similar Artists",
                        style = MaterialTheme.typography.headlineSmall,
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
    onTrackSelected: (SpotifyTrack) -> Unit
) {
    val thumbnailUrl =
        if (track.album.images.size >= 2)
            track.album.images[1].url
        else
            track.album.images.firstOrNull()?.url
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onTrackSelected(track)
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl) // Use thumbnail if available, otherwise fallback to larger image
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp),
                    contentScale = ContentScale.Crop
                )
            }
            else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = "No album image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column (
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = track.artists.joinToString(", ") { it.name },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = track.album.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ArtistSuggestionItem(
    artist: SpotifyArtist,
    onArtistSelected: (SpotifyArtist) -> Unit
) {
    val thumbnailUrl =
        artist.images?.size?.let {
            if (it >= 2)
                artist.images[1].url
            else
                artist.images.firstOrNull()?.url
        }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onArtistSelected(artist)
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl) // Use thumbnail if available, otherwise fallback to larger image
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp),
                    contentScale = ContentScale.Crop
                )
            }
            else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = "No album image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column (
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = artist.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                val followers = artist.followers["total"]
                val formattedFollowers = followers.toString().reversed().chunked(3).joinToString(",").reversed()
                Text(
                    text = "Followers: $formattedFollowers",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = artist.genres.joinToString (", "),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun AlbumSuggestionItem(
    album: SpotifyAlbum,
    onAlbumSelected: (SpotifyAlbum) -> Unit
) {
    val thumbnailUrl =
        if (album.images.size >= 2)
            album.images[1].url
        else
            album.images.firstOrNull()?.url
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onAlbumSelected(album)
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl) // Use thumbnail if available, otherwise fallback to larger image
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp),
                    contentScale = ContentScale.Crop
                )
            }
            else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = "No album image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column (
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = album.artists.joinToString(", ") { it.name },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = album.releaseDate,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SimplifiedTrackSuggestionItem(
    selectedAlbum: SpotifyAlbum?,
    track: SimplifiedTrack,
    onTrackSelected: (SimplifiedTrack) -> Unit
) {
    val thumbnailUrl =
        selectedAlbum?.images?.size?.let {
            if (it >= 2)
                selectedAlbum.images[1].url
            else
                selectedAlbum.images.firstOrNull()?.url
        }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                onTrackSelected(track)
            }
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(thumbnailUrl) // Use thumbnail if available, otherwise fallback to larger image
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Image",
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp),
                    contentScale = ContentScale.Crop
                )
            }
            else {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(108.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant), // Placeholder background
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Album,
                        contentDescription = "No album image available",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column (
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Text(
                    text = track.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = track.artists.joinToString(", ") { it.name },
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = selectedAlbum?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
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
//    val state = SearchUiState.Success(SpotifyDataList(listOf(), listOf(), listOf()))
//    val suggestedState = SearchUiState.Success(SpotifyDataList(listOf(), listOf(), listOf()))

    //HomePage(state, suggestedState, "", "", {}, {}, {}, {}, null, null, {}, {}, false, {}, false, {}, { _, _ -> /* Mock searchSimilarTracksAndArtists */})
}
