package com.example.geminispotifyapp.presentation.features.main.findmusic

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val similarUiState by viewModel.searchSimilarUiState.collectAsStateWithLifecycle()
    val trackInput by viewModel.trackInput.collectAsStateWithLifecycle()
    val artistInput by viewModel.artistInput.collectAsStateWithLifecycle()
    val dataInput by viewModel.dataInput.collectAsStateWithLifecycle()

    val suggestedUiState by viewModel.searchDataUiState.collectAsStateWithLifecycle()
    val selectedSuggestedTrack by viewModel.selectedSuggestedTrack.collectAsStateWithLifecycle()
    val hasSelectedTrackAndInputDoesNotChange by viewModel.hasSelectedTrackAndInputDoesNotChange.collectAsStateWithLifecycle()
    val hasSelectedArtistAndInputDoesNotChange by viewModel.hasSelectedArtistAndInputDoesNotChange.collectAsStateWithLifecycle()
    val hasSelectedDataAndInputDoesNotChange by viewModel.hasSelectedDataAndInputDoesNotChange.collectAsStateWithLifecycle()
    val hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange by viewModel.hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange.collectAsStateWithLifecycle()

    val searchByIdUiState by viewModel.searchByIdUiState.collectAsStateWithLifecycle()
    val selectedAlbum by viewModel.selectedAlbum.collectAsStateWithLifecycle()

    val searchButtonAnimationTrigger by viewModel.searchButtonAnimationTrigger.collectAsStateWithLifecycle()


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
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Find Music",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        shadow = Shadow(
                            color = SpotifyGreen.copy(alpha = 0.5f),
                            blurRadius = 30f
                        )
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = SpotifyWhite,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "POWERED BY SPOTIFY & GEMINI",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpotifyGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select a track below to find similar vibes tailored for you.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            Spacer(Modifier.padding(4.dp))
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    SearchTextField(
                        value = dataInput,
                        onValueChange = {
                            onDataInputChange(it)
                            onHasSelectedDataAndInputDoesNotChangeSet(false)
                            onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false)
                        },
                        label = "Search Everything (Track, Artist, Album)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SearchTextField(
                            value = trackInput,
                            onValueChange = {
                                onTrackInputChange(it)
                                onHasSelectedTrackAndInputDoesNotChangeSet(false)
                                onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false)
                            },
                            label = "Track Name",
                            modifier = Modifier.weight(1f)
                        )

                        SearchTextField(
                            value = artistInput,
                            onValueChange = {
                                onArtistInputChange(it)
                                onHasSelectedArtistAndInputDoesNotChangeSet(false)
                                onHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChangeSet(false)
                            },
                            label = "Artist Name",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
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
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SpotifyGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = SpotifyGreen,
            cursorColor = SpotifyGreen
        ),
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Clear text",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
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
