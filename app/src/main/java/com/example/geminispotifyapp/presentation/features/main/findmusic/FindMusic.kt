package com.example.geminispotifyapp.presentation.features.main.findmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.remote.model.SimplifiedTrack
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.remote.model.TrackInformation
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.topartists.ArtistItem
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.toptracks.TrackItem
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyGreen
import com.example.geminispotifyapp.presentation.ui.theme.SpotifyWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FindMusicScreen(
    onArtistClick: (SpotifyArtist) -> Unit, 
    onTrackClick: (SpotifyTrack) -> Unit, 
    viewModel: FindMusicViewModel
) { 
    val similarUiState by viewModel.searchSimilarUiState.collectAsState()
    val trackInput by viewModel.trackInput.collectAsState()
    val artistInput by viewModel.artistInput.collectAsState()
    val dataInput by viewModel.dataInput.collectAsState()

    val suggestedUiState by viewModel.searchDataUiState.collectAsState()
    val selectedSuggestedTrack by viewModel.selectedSuggestedTrack.collectAsState()
    val hasSelectedTrackAndInputDoesNotChange by viewModel.hasSelectedTrackAndInputDoesNotChange.collectAsState()
    val hasSelectedArtistAndInputDoesNotChange by viewModel.hasSelectedArtistAndInputDoesNotChange.collectAsState()
    val hasSelectedDataAndInputDoesNotChange by viewModel.hasSelectedDataAndInputDoesNotChange.collectAsState()
    val hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange by viewModel.hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange.collectAsState()

    val searchByIdUiState by viewModel.searchByIdUiState.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    val searchButtonAnimationTrigger by viewModel.searchButtonAnimationTrigger.collectAsState()


    FindMusicPage(
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
            viewModel.searchTrack(newTrack, FindMusicViewModel.Type.TRACK, FindMusicViewModel.Type.TRACK) },
        { newArtist ->
            viewModel.onArtistInputChange(newArtist)
            viewModel.searchTrack(newArtist, FindMusicViewModel.Type.ARTIST, FindMusicViewModel.Type.TRACK) }, //TODO: more search for artist
        { newData ->
            viewModel.onDataInputChange(newData)
            viewModel.searchTrack(newData, FindMusicViewModel.Type.ALLMENTIONED, FindMusicViewModel.Type.ALLMENTIONED)},
        selectedSuggestedTrack,
        { track -> viewModel.onSelectedSuggestedTrackChange(track) },
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
        { trackId -> viewModel.getTrackAndSelectedTrack(trackId) },
        searchButtonAnimationTrigger,
        { viewModel.setSearchButtonAnimationTriggerToInitial() },
        { viewModel.checkIfFullyInput() }
    )
}

@Composable
fun FindMusicPage(
    uiState: UiState<SpotifyDataList>,
    suggestedUiState: UiState<SpotifyDataList>,
    searchByIdUiState: UiState<SpotifyDataList>,
    trackInput: String,
    artistInput: String,
    dataInput: String,
    onArtistClick: (SpotifyArtist) -> Unit,
    onTrackClick: (SpotifyTrack) -> Unit,
    onTrackInputChange: (String) -> Unit,
    onArtistInputChange: (String) -> Unit,
    onDataInputChange: (String) -> Unit,
    selectedSuggestedTrack: SpotifyTrack?,
    onSelectedSuggestedTrackChange: (SpotifyTrack?) -> Unit,
    hasSelectedTrackAndInputDoesNotChange: Boolean,
    onHasSelectedTrackAndInputDoesNotChangeSet: (Boolean) -> Unit,
    hasSelectedArtistAndInputDoesNotChange: Boolean,
    onHasSelectedArtistAndInputDoesNotChangeSet: (Boolean) -> Unit,
    hasSelectedDataAndInputDoesNotChange: Boolean,
    onHasSelectedDataAndInputDoesNotChangeSet: (Boolean) -> Unit,
    hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange: Boolean,
    onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet: (Boolean) -> Unit,
    searchSimilarTracksAndArtists: (String, String) -> Unit,
    getTopTracksOfArtist: (String) -> Unit,
    selectedAlbum: SpotifyAlbum?,
    onSetSelectedAlbum: (SpotifyAlbum?) -> Unit,
    getAlbumTracks: (String) -> Unit,
    getTrackAndSelectedTrack: (String) -> Unit,
    searchButtonAnimationTrigger: Int,
    onAnimationComplete: () -> Unit,
    checkIfFullyInput: () -> Boolean
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var isSearchButtonHighlighted by remember { mutableStateOf(false) }

    LaunchedEffect(searchButtonAnimationTrigger) {
        if (searchButtonAnimationTrigger > 0) {
            isSearchButtonHighlighted = true
            delay(1000L)
            isSearchButtonHighlighted = false
            onAnimationComplete()
        }
    }

    val animatedButtonBackgroundColor by animateColorAsState(
        targetValue = if (isSearchButtonHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        animationSpec = tween(durationMillis = 300),
        label = "Button Background Color Animation"
    )
    val animatedButtonScale by animateFloatAsState(
        targetValue = if (isSearchButtonHighlighted) 1.05f else 1.0f,
        animationSpec = tween(durationMillis = 300),
        label = "Button Scale Animation"
    )
    val animatedButtonTextColor by animateColorAsState(
        targetValue = if (isSearchButtonHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 300),
        label = "Button Text Color Animation"
    )

    val textStyleWithShadow = MaterialTheme.typography.headlineSmall.copy(
        shadow = Shadow(
            color = Color.LightGray.copy(alpha = 0.5f),
            blurRadius = 20f
        )
    )
    val textStyleWithShadowLabel = MaterialTheme.typography.labelLarge.copy(
        shadow = Shadow(
            color = Color.LightGray.copy(alpha = 0.5f),
            blurRadius = 20f
        )
    )

    LazyColumn(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp, 0.dp, 6.dp, 12.dp),
        contentPadding = PaddingValues(bottom = 80.dp) // For the bottom navigation bar
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
                label = { Text("Input Any Name (of track, artist, or album)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                modifier = Modifier.fillMaxWidth()
            ) {
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
                    label = { Text("Track Name") },
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
                    label = { Text("Artist Name") },
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
        if (suggestedUiState is UiState.Success) {
            suggestedData = suggestedUiState.data
            suggestedTracks = suggestedData.tracks
            suggestedArtists = suggestedData.artists
            suggestedAlbums = suggestedData.albums
        }

        var suggestedTrackById: List<TrackInformation>? = null
        if (searchByIdUiState is UiState.Success) {
            suggestedTrackById = searchByIdUiState.data.trackInformation
        }
        suggestedTracks?.let { tracks ->
            if (((trackInput.isNotBlank() && !hasSelectedTrackAndInputDoesNotChange) ||
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
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Track Suggestions",
                                    style = textStyleWithShadow,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpotifyGreen
                                )
                                if (tracks.isEmpty()) {
                                    Text(
                                        text = "No suggested tracks found.",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                items(
                    items = tracks,
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
        }
        suggestedArtists?.let { artists ->
            if (dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange) {
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Artist Suggestions",
                                style = textStyleWithShadow,
                                fontFamily = FontFamily.Monospace,
                                color = SpotifyGreen
                            )
                            if (artists.isEmpty()) {
                                Text(
                                    text = "No suggested artists found.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(
                    items = artists,
                    key = { artist -> artist.id }
                ) {
                    ArtistSuggestionItem(
                        artist = it,
                        onArtistSelected = { selectedArtist ->
                            onArtistInputChange(selectedArtist.name)
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
        }
        suggestedAlbums?.let { albums ->
            if (dataInput.isNotBlank() && !hasSelectedDataAndInputDoesNotChange) {
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Album Suggestions",
                                style = textStyleWithShadow,
                                fontFamily = FontFamily.Monospace,
                                color = SpotifyGreen
                            )
                            if (albums.isEmpty()) {
                                Text(
                                    text = "No suggested albums found.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                items(
                    items = albums,
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
                    Column (horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Track Suggestions",
                            style = textStyleWithShadow,
                            fontFamily = FontFamily.Monospace,
                            color = SpotifyGreen
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (suggestedTrackById!![0] is SpotifyTrack) {
                                Text(
                                    text = "Top Tracks of Artist",
                                    style = textStyleWithShadowLabel,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpotifyWhite
                                )
                            }
                            else if (suggestedTrackById!![0] is SimplifiedTrack) {
                                Text(
                                    text = "Tracks from Album",
                                    style = textStyleWithShadowLabel,
                                    fontFamily = FontFamily.Monospace,
                                    color = SpotifyWhite
                                )
                            }
                        }
                    }
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
                            onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(true)
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
                    suggestedData = null
                    suggestedTracks = null
                    suggestedArtists = null
                    suggestedAlbums = null
                    suggestedTrackById = null
                    if (checkIfFullyInput()) {
                        scope.launch {
                            searchSimilarTracksAndArtists(
                                trackInput,
                                artistInput
                            )
                        }
                    }
                    focusManager.clearFocus()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedButtonBackgroundColor
                ),
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .scale(animatedButtonScale)
                    .shadow(
                        elevation = if (isSearchButtonHighlighted) 8.dp else 4.dp,
                        shape = RoundedCornerShape(50)
                    )
            ) {
                if (uiState != UiState.Loading) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search Icon",
                        modifier = Modifier.size(20.dp),
                        tint = animatedButtonTextColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Search similar tracks and artists!",
                        color = animatedButtonTextColor,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Loading... (Tap to Cancel)",
                        color = animatedButtonTextColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        item {
            selectedSuggestedTrack?.let { track ->
                Spacer(Modifier.padding(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "Selected Track",
                        style = textStyleWithShadow,
                        fontFamily = FontFamily.Monospace,
                        color = SpotifyGreen
                    )
                }
                TrackItem(index = 0, track = track) { onTrackClick(it) }
            }
        }
        when (uiState) {
            is UiState.Loading -> {
                item {
                    LoadingContent()
                }
            }

            is UiState.Success -> {
                val artists = uiState.data.artists
                val tracks = uiState.data.tracks
                item {
                    Spacer(Modifier.padding(8.dp, 8.dp, 8.dp, 4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = "Similar Tracks",
                            style = textStyleWithShadow,
                            fontFamily = FontFamily.Monospace,
                            color = SpotifyGreen
                        )
                    }
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ){
                        Text(
                            text = "Similar Artists",
                            style = textStyleWithShadow,
                            fontFamily = FontFamily.Monospace,
                            color = SpotifyGreen
                        )
                    }
                }
                if (artists != null) {
                    item {
                        Spacer(Modifier.padding(4.dp))
                        Column {
                            artists.forEachIndexed { index, artist ->
                                ArtistItem(index + 1, artist) { onArtistClick(it) }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                } else {
                    item { Text("No artists found") }
                }
            }

            is UiState.Error -> {
//                item {
//                    Text(
//                        "Error",
//                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
//                        textAlign = TextAlign.Center
//                    )
//                }
            }

            else -> {
                //Text("Initial")
            }
        }
    }
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


//@Composable
//private fun HomeNavigation() {
//    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val activity = LocalContext.current as? Activity
//
//    val backCallback = remember {
//        object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                activity?.finish()
//            }
//        }
//    }
//
//    DisposableEffect (lifecycleOwner, backDispatcher) {
//        backDispatcher?.addCallback(lifecycleOwner, backCallback)
//        onDispose {
//            backCallback.remove()
//        }
//    }
//}

@Composable
fun LoadingContent() {
    Box(Modifier
        .fillMaxSize()
        .padding(32.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Preview
@Composable
fun FindMusicPagePreview() {
    val mockUiState = UiState.Success(SpotifyDataList(emptyList(), emptyList(), emptyList(), emptyList()))
    val mockSuggestedUiState = UiState.Success(SpotifyDataList(emptyList(), emptyList(), emptyList(), emptyList()))
    val mockSearchByIdUiState = UiState.Success(SpotifyDataList(emptyList(), emptyList(), emptyList(), emptyList()))

    FindMusicPage(
        uiState = mockUiState,
        suggestedUiState = mockSuggestedUiState,
        searchByIdUiState = mockSearchByIdUiState,
        trackInput = "Bohemian Rhapsody",
        artistInput = "Queen",
        dataInput = "",
        onArtistClick = {},
        onTrackClick = {},
        onTrackInputChange = {},
        onArtistInputChange = {},
        onDataInputChange = {},
        selectedSuggestedTrack = null,
        onSelectedSuggestedTrackChange = {},
        hasSelectedTrackAndInputDoesNotChange = false,
        onHasSelectedTrackAndInputDoesNotChangeSet = {},
        hasSelectedArtistAndInputDoesNotChange = false,
        onHasSelectedArtistAndInputDoesNotChangeSet = {},
        hasSelectedDataAndInputDoesNotChange = false,
        onHasSelectedDataAndInputDoesNotChangeSet = {},
        hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange = false,
        onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet = {},
        searchSimilarTracksAndArtists = { _, _ -> },
        getTopTracksOfArtist = {},
        selectedAlbum = null,
        onSetSelectedAlbum = {},
        getAlbumTracks = {},
        getTrackAndSelectedTrack = {},
        searchButtonAnimationTrigger = 0,
        onAnimationComplete = {},
        checkIfFullyInput = { true },
    )
}
