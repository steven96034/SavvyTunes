package com.example.geminispotifyapp.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyDataList
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyAlbum
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.UiEvent
import com.example.geminispotifyapp.features.UiEventManager
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import com.example.geminispotifyapp.utils.GlobalErrorHandler
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.ServerException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val uiEventManager: UiEventManager,
    private val globalErrorHandler: GlobalErrorHandler
) : ViewModel() {
    // For Search State
    private var _searchSimilarUiState: MutableStateFlow<SearchUiState> =
        MutableStateFlow(SearchUiState.Initial)
    val searchSimilarUiState: StateFlow<SearchUiState> = _searchSimilarUiState.asStateFlow()

    private var _searchDataUiState: MutableStateFlow<SearchUiState> =
        MutableStateFlow(SearchUiState.Initial)
    val searchDataUiState: StateFlow<SearchUiState> = _searchDataUiState.asStateFlow()

    private var _searchByIdUiState: MutableStateFlow<SearchUiState> =
        MutableStateFlow(SearchUiState.Initial)
    val searchByIdUiState: StateFlow<SearchUiState> = _searchByIdUiState.asStateFlow()

    // For Gemini API
    private var relatedTracks = mutableListOf<String>()
    private var relatedArtists = mutableListOf<String>()

    // For Spotify API (Not-Found List is for debug.)
    private val trackTempList: MutableList<SpotifyTrack> = mutableListOf()
    private val trackNotFoundList: MutableList<String> = mutableListOf()

    private val artistTempList: MutableList<SpotifyArtist> = mutableListOf()
    private val artistNotFoundList: MutableList<String> = mutableListOf()

    // For Input Data (Revealed in Layout)
    private var _trackInput = MutableStateFlow("")
    val trackInput: StateFlow<String> = _trackInput.asStateFlow()

    private var _artistInput = MutableStateFlow("")
    val artistInput: StateFlow<String> = _artistInput.asStateFlow()

    private var _dataInput = MutableStateFlow("")
    val dataInput: StateFlow<String> = _dataInput.asStateFlow()

    private var _selectedSuggestedTrack: MutableStateFlow<SpotifyTrack?> = MutableStateFlow(null)
    val selectedSuggestedTrack: StateFlow<SpotifyTrack?> = _selectedSuggestedTrack.asStateFlow()

    private var _hasSelectedTrackAndInputDoesNotChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val hasSelectedTrackAndInputDoesNotChange: StateFlow<Boolean> =
        _hasSelectedTrackAndInputDoesNotChange.asStateFlow()

    private var _selectedSuggestedArtist: MutableStateFlow<SpotifyArtist?> = MutableStateFlow(null)
    val selectedSuggestedArtist: StateFlow<SpotifyArtist?> = _selectedSuggestedArtist.asStateFlow()

    private var _hasSelectedArtistAndInputDoesNotChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val hasSelectedArtistAndInputDoesNotChange: StateFlow<Boolean> =
        _hasSelectedArtistAndInputDoesNotChange.asStateFlow()

    private var _hasSelectedDataAndInputDoesNotChange: MutableStateFlow<Boolean> =
        MutableStateFlow(false)
    val hasSelectedDataAndInputDoesNotChange: StateFlow<Boolean> =
        _hasSelectedDataAndInputDoesNotChange.asStateFlow()

    private var _selectedAlbum: MutableStateFlow<SpotifyAlbum?> = MutableStateFlow(null)
    val selectedAlbum: StateFlow<SpotifyAlbum?> = _selectedAlbum.asStateFlow()

    private val _hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange = MutableStateFlow(false)
    val hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange: StateFlow<Boolean> =
        _hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange.asStateFlow()

    fun setHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange(newValue: Boolean) {
        _hasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange.value = newValue
    }

    private val _searchButtonAnimationTrigger = MutableStateFlow(0)
    val searchButtonAnimationTrigger: StateFlow<Int> = _searchButtonAnimationTrigger.asStateFlow()

    val searchSimilarNum: StateFlow<Int> = spotifyRepository.searchSimilarNumFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 20
        )

    private val tag = "HomeViewModel"

    private var searchInputJob: Job? = null
    private var searchCount = 0

    // Debounce time in milliseconds, to prevent multiple requests being sent too quickly
    private val debouncePeriod: Long = 300L

    enum class Type {
        TRACK,
        ARTIST,
        ALBUM,
        ALLMENTIONED
    }

    fun onTrackInputChange(newTrack: String) {
        _trackInput.value = newTrack
    }

    fun onArtistInputChange(newArtist: String) {
        _artistInput.value = newArtist
    }

    fun onDataInputChange(newData: String) {
        _dataInput.value = newData
    }

    fun onSelectedSuggestedTrackChange(track: SpotifyTrack?) {
        _selectedSuggestedTrack.value = track
        _searchSimilarUiState.value = SearchUiState.Initial
        _searchByIdUiState.value = SearchUiState.Initial
        _searchButtonAnimationTrigger.value++
    }

    fun onHasSelectedTrackAndInputDoesNotChangeSet(set: Boolean) {
        _hasSelectedTrackAndInputDoesNotChange.value = set
    }

    fun onSelectedSuggestedArtistChange(artist: SpotifyArtist?) {
        _selectedSuggestedArtist.value = artist
    }

    fun onHasSelectedArtistAndInputDoesNotChangeSet(set: Boolean) {
        _hasSelectedArtistAndInputDoesNotChange.value = set
    }

    fun onHasSelectedDataAndInputDoesNotChangeSet(set: Boolean) {
        _hasSelectedDataAndInputDoesNotChange.value = set
    }

    // For Search Button Animation, set to 0 after clicking search button.
    fun setSearchButtonAnimationTriggerToInitial() {
        _searchButtonAnimationTrigger.value = 0
    }

    fun checkIfFullyInput(): Boolean {
        if (trackInput.value.isNotBlank() && artistInput.value.isNotBlank())
            return true
        uiEventManager.sendEvent(UiEvent.ShowSnackbar("Please input both track and artist."))
        return false
    }

    fun searchTrack(searchName: String, byType: Type = Type.TRACK, searchFor: Type = Type.TRACK) {
        if (searchName.isBlank()) return

        if (_searchDataUiState.value is SearchUiState.Loading) {
            searchInputJob?.cancel(CancellationException("New search by user."))
            Log.d(tag, "New search by user. Total searchCount so far: $searchCount")
        }

        searchInputJob = viewModelScope.launch {
            delay(debouncePeriod)
            ++searchCount
            _searchDataUiState.value = SearchUiState.Loading
            try {
                val query = when (byType) {
                    Type.TRACK -> "track:\"$searchName\""
                    Type.ARTIST -> "artist:\"$searchName\""
                    Type.ALBUM -> "album:\"$searchName\"" // TBD
                    Type.ALLMENTIONED -> searchName
                }
                val type = when (searchFor) {
                    Type.TRACK -> "track"
                    Type.ARTIST -> "artist"
                    Type.ALBUM -> "album" // TBD
                    Type.ALLMENTIONED -> "track,artist,album" // TODO: ""
                }

                val result = spotifyRepository.searchData(query, type, 10)

                when (result) {
                    is FetchResult.Success -> {
                        val response = result.data
                        when (searchFor) {
                            Type.TRACK -> {
                                if (response.tracks == null || response.tracks.items.isEmpty()) {
                                    _searchDataUiState.value = SearchUiState.Error("No tracks found")
                                    return@launch
                                }
                                val tracks = response.tracks.items
                                _searchDataUiState.value =
                                    SearchUiState.Success((SpotifyDataList(tracks, null, null, null)))
                            }

                            Type.ARTIST -> {
                                if (response.artists == null || response.artists.items.isEmpty()) {
                                    _searchDataUiState.value = SearchUiState.Error("No artists found")
                                    return@launch
                                }
                                val artists = response.artists.items
                                _searchDataUiState.value =
                                    SearchUiState.Success((SpotifyDataList(null, artists, null, null)))
                            }

                            Type.ALBUM -> {
                                if (response.albums == null || response.albums.items.isEmpty()) {
                                    _searchDataUiState.value = SearchUiState.Error("No albums found")
                                    return@launch
                                }
                                val albums = response.albums.items
                                _searchDataUiState.value =
                                    SearchUiState.Success((SpotifyDataList(null, null, albums, null)))
                            }

                            Type.ALLMENTIONED -> {
                                if ((response.tracks == null || response.tracks.items.isEmpty()) &&
                                    (response.artists == null || response.artists.items.isEmpty()) &&
                                    (response.albums == null || response.albums.items.isEmpty())
                                ) {
                                    _searchDataUiState.value = SearchUiState.Error("No data found")
                                    return@launch
                                }
                                val tracks = response.tracks?.items
                                val artists = response.artists?.items
                                val albums = response.albums?.items
                                _searchDataUiState.value =
                                    SearchUiState.Success((SpotifyDataList(tracks, artists, albums, null)))
                            }
                        }
                    }
                    is FetchResult.Error -> {
                        handleApiError(result.errorData)
                        _searchDataUiState.value = SearchUiState.Error(result.errorData.message ?: "Some Error Happened...")
                    }
                    else -> {}
                }
                Log.d(tag, "Search for \"$searchName\" completed.")
            } catch (e: CancellationException) {
                Log.d(tag, "Search for \"$searchName\" was cancelled: ${e.message}")
                //_searchDataUiState.value = SearchUiState.Initial
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when searching for data, please try again later.", e.stackTraceToString()
                    )
                )
                _searchDataUiState.value =
                    SearchUiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }


    fun getTopTracksOfArtist(artistId: String) {
        if (artistId.isBlank()) return
        viewModelScope.launch {
            _searchByIdUiState.value = SearchUiState.Loading
            try {
                val result = spotifyRepository.getTopTracksOfArtist(artistId)

                when (result) {
                    is FetchResult.Success -> {
                        val response = result.data
                        if (response.tracks.isEmpty()) {
                            _searchByIdUiState.value = SearchUiState.Error("No tracks found in this return list")
                            uiEventManager.sendEvent(UiEvent.ShowSnackbar("No tracks found from the artist."))
                            return@launch
                        }
                        Log.d(tag, "response: $response")
                        _searchByIdUiState.value =
                            SearchUiState.Success((SpotifyDataList(null, null, null, response.tracks)))
                    }
                    is FetchResult.Error -> {
                        val errorMessage = result.errorData.message ?: "Some Error Happened..."
                        handleApiError(result.errorData)
                        _searchByIdUiState.value = SearchUiState.Error(errorMessage)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when getting top tracks of the artist, please try again later.", e.stackTraceToString()
                    )
                )
                _searchByIdUiState.value = SearchUiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }

    fun getAlbumTracks(albumId: String) {
        if (albumId.isBlank()) return
        viewModelScope.launch {
            _searchByIdUiState.value = SearchUiState.Loading
            try {
                val result = spotifyRepository.getAlbumTracks(albumId)

                when (result) {
                    is FetchResult.Success -> {
                        val response = result.data
                        if (response.tracks.isEmpty()) {
                            _searchByIdUiState.value = SearchUiState.Error("No tracks found in this album")
                            uiEventManager.sendEvent(UiEvent.ShowSnackbar("No tracks found in this album."))
                            return@launch
                        }
                        _searchByIdUiState.value =
                            SearchUiState.Success((SpotifyDataList(null, null, null, response.tracks)))
                    }
                    is FetchResult.Error -> {
                        val errorMessage = result.errorData.message ?: "Some Error Happened..."
                        handleApiError(result.errorData)
                        _searchByIdUiState.value = SearchUiState.Error(errorMessage)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when getting tracks of the album, please try again later.", e.stackTraceToString()
                    )
                )
                _searchByIdUiState.value = SearchUiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }

    fun onSetSelectedAlbum(album: SpotifyAlbum?) {
        _selectedAlbum.value = album
    }

//    fun getTrack(trackId: String) {
//        if (trackId.isBlank()) return
//        viewModelScope.launch {
//            _searchByIdUiState.value = SearchUiState.Loading
//            try {
//                val response = spotifyRepositoryImpl.getTrack(trackId)
////                if (response == null) {
////                    _searchByIdUiState.value = SearchUiState.Error("No track information found")
////                    return@launch
////                }
//                _searchByIdUiState.value = SearchUiState.Success((SpotifyDataList(listOf(response), null, null, null)))
//            } catch (e: Exception) {
//                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
//                _searchByIdUiState.value = SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
//            }
//        }
//    }

    fun getTrackAndSelectedTrack(trackId: String) {
        if (trackId.isBlank()) return
        viewModelScope.launch {
            try {
                val result = spotifyRepository.getTrack(trackId)
                when (result) {
                    is FetchResult.Success -> {
                        onSelectedSuggestedTrackChange(result.data)
                    }
                    is FetchResult.Error -> {
                        handleApiError(result.errorData)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when getting track data, please try again later.", e.stackTraceToString()
                    )
                )
                Log.e(tag, "Error occurred during getTrackAndSelectedTrack($trackId): ${e.message}")
            }
        }
    }

//    fun searchArtist(artistName: String) {
//        if (artistName.isBlank()) return
//
//        viewModelScope.launch {
//            _searchDataUiState.value = SearchUiState.Loading
//            try {
//                val response = spotifyRepositoryImpl.searchData("artist:$artistName", "artist", 10)
//                if (response.artists == null || response.artists.items.isEmpty()) {
//                    _searchDataUiState.value = SearchUiState.Error("No artists found")
//                    return@launch
//                }
//                // 處理前十個結果
////                val artists = response.tracks!!.items
////                    .take(10)
////                    .flatMap { it.artists }
////                    .map { it.name }
////                    .distinct()//移除重複的名稱
//                val artists = response.artists.items
//                _searchDataUiState.value =
//                    SearchUiState.Success((SpotifyDataList(null, artists, null, null)))
//            } catch (e: Exception) {
//                uiEventManager.sendEvent(
//                    UiEvent.ShowSnackbar(
//                        e.localizedMessage ?: "Some Error Happened..."
//                    )
//                )
//                _searchDataUiState.value =
//                    SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
//            }
//        }
//    }


    // (Pass from Gemini response)
    // Here check the equality of track name, album name and artist name, then calculate the most similar track.
    // To get more reachable in spotify search, due to the miswrite of album name of the track from Gemini response,
    //      we need to just search by the track name and artist name but more selection criteria (TBD).
//    private suspend fun searchForSpecificTrack(
//        trackName: String,
//        albumName: String,
//        artistName: String
//    ) {
//        //lateinit var response: SearchResponse
//        var query: String? = ""
//        try {
//            if (trackName.isNotEmpty()) query += "track:\"$trackName\""
//            if (albumName.isNotEmpty()) query += " album:\"$albumName\""
//            if (artistName.isNotEmpty()) query += " artist:\"$artistName\""
//            Log.d(tag, "Query: $query")
//            if (query.isNullOrEmpty()) {
//                Log.d(tag, "Query is empty. No search performed.")
//                return
//            }
//            val result = spotifyRepositoryImpl.searchData(query, "track")
//
//            when (result) {
//                is FetchResult.Success -> {
//                    // Explicitly check for null and assign to a local variable
//                    val response = result.data
//                    val tracks = response.tracks
//                    if (tracks == null || tracks.total == 0) {
//                        // Handle the case where response.tracks is null
//                        trackNotFoundList.add("$trackName by $artistName")
//                        Log.e(tag, "response.tracks is null")
//                        return
//                    }
//                    //val candidateTempList = mutableListOf<SpotifyTrack>() // For further filtering
//                    val foundTrack = tracks.items.take(20).find { track ->
//                        track.name.equals(trackName, ignoreCase = true) &&
//                                track.album.name.equals(albumName, ignoreCase = true) &&
//                                track.artists.any { artist ->
//                                    artist.name.equals(artistName, ignoreCase = true)
//                                }
//                    }
//                    if (foundTrack != null) {
//                        trackTempList.add(foundTrack)
//                        Log.d(tag, "Track found: ${foundTrack.name}")
//                    } else {
//                        //find the most closed track name
//                        val mostSimilarTrack =
//                            tracks.items.maxByOrNull { track -> // Use the local 'tracks' variable
//                                val trackNameSimilarity = calculateSimilarity(track.name, trackName)
//                                val albumNameSimilarity =
//                                    calculateSimilarity(track.album.name, albumName)
//                                val artistNameSimilarity = track.artists.maxOfOrNull { artist ->
//                                    calculateSimilarity(artist.name, artistName)
//                                } ?: 0.0
//                                trackNameSimilarity * 1.5 + albumNameSimilarity * 2 + artistNameSimilarity
//                            }
//                        if (mostSimilarTrack != null) {
//                            trackTempList.add(mostSimilarTrack)
//                            Log.d(
//                                tag,
//                                "Track not found: $trackName, but similar track found: ${mostSimilarTrack.name}"
//                            )
//                        } else {
//                            trackNotFoundList.add("$trackName by $artistName")
//                            Log.e(tag, "Track not found: $trackName")
//                        }
//                    }
//                }
//                // What should i do if the track is not found?
//                is FetchResult.Error -> {
//                    // No need to handle error for UI snackbar here, this function it's just a part of searchSimilarTracksAndArtists
//                    // handleApiError(result.errorData)
//                    // _searchDataUiState.value = SearchUiState.Error(result.errorData.message ?: "Some Error Happened...")
//                    Log.e(tag, "Error occurred during searchForSpecificTrack($query): ${result.errorData.message}")
//                }
//                else -> {}
//            }
//        } catch (e: Exception) {
//            // No need to handle error here, this function it's just a part of searchSimilarTracksAndArtists
//            Log.e(tag, "Error occurred during searchForSpecificTrack($query): ${e.message}")
//            //throw e
//        }
//    }
    private suspend fun searchForSpecificTrack(
        trackName: String,
        albumName: String,
        artistName: String
    ): Pair<SpotifyTrack?, String?> { // 修改回傳型別為 Pair<SpotifyTrack?, String?>
        var query: String? = ""
        try {
            if (trackName.isNotEmpty()) query += "track:\"$trackName\""
            if (albumName.isNotEmpty()) query += " album:\"$albumName\""
            if (artistName.isNotEmpty()) query += " artist:\"$artistName\""
            Log.d(tag, "Query: $query")
            if (query.isNullOrEmpty()) {
                Log.d(tag, "Query is empty. No search performed.")
                return Pair(null, "$trackName by $artistName (Null query)")
            }
            val result = spotifyRepository.searchData(query, "track")

            return when (result) {
                is FetchResult.Success -> {
                    // Explicitly check for null and assign to a local variable
                    val response = result.data
                    val tracks = response.tracks
                    if (tracks == null || tracks.total == 0) {
                        Log.e(tag, "response.tracks is null or empty for query: $query")
                        return Pair(null, "$trackName by $artistName")
                    }
                    //val candidateTempList = mutableListOf<SpotifyTrack>() // For further filtering
                    val foundTrack = tracks.items.take(20).find { track ->
                        track.name.equals(trackName, ignoreCase = true) &&
                                track.album.name.equals(albumName, ignoreCase = true) &&
                                track.artists.any { artist ->
                                    artist.name.equals(artistName, ignoreCase = true)
                                }
                    }
                    if (foundTrack != null) {
                        Log.d(tag, "Track found: ${foundTrack.name}")
                        Pair(foundTrack, null)
                    } else {
                        //find the most closed track name
                        val mostSimilarTrack =
                            tracks.items.maxByOrNull { track -> // Use the local 'tracks' variable
                                val trackNameSimilarity = calculateSimilarity(track.name, trackName)
                                val albumNameSimilarity =
                                    calculateSimilarity(track.album.name, albumName)
                                val artistNameSimilarity = track.artists.maxOfOrNull { artist ->
                                    calculateSimilarity(artist.name, artistName)
                                } ?: 0.0
                                trackNameSimilarity * 1.5 + albumNameSimilarity * 2 + artistNameSimilarity
                            }
                        if (mostSimilarTrack != null) {
                            Log.d(
                                tag,
                                "Track not found: $trackName, but similar track found: ${mostSimilarTrack.name}"
                            )
                            Pair(mostSimilarTrack, null)
                        } else {
                            Log.e(tag, "Track not found: $trackName")
                            Pair(null, "$trackName by $artistName")
                        }
                    }
                }
                is FetchResult.Error -> {
                    Log.e(tag, "Error occurred during searchForSpecificTrack($query): ${result.errorData.message}")
                    Pair(null, "$trackName by $artistName (API Error: ${result.errorData.message})")
                }
                else -> {
                    Log.e(tag, "Unexpected FetchResult state for searchForSpecificTrack($query)")
                    Pair(null, "$trackName by $artistName (Unexpected)")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during searchForSpecificTrack($query): ${e.message}")
            return Pair(null, "$trackName by $artistName (Exception: ${e.message})")
        }
    }

    // (Pass from Gemini response)
    // To simply get artists, we only get the artists with enough popularity.
    // (More precise still wait for translation of artist name... some modification of prompt in the future.)
    // (Or TBD for searching the tracks or albums of the artists to get most precise artists.)
    private suspend fun searchForSpecificArtists(artistName: String): Pair<SpotifyArtist?, String?> {
        try {
            val result = spotifyRepository.searchData("artist:\"$artistName\"", "artist")

            return when (result) {
                is FetchResult.Success -> {
                    val response = result.data
                    val mostSimilarArtist = response.artists?.items?.maxByOrNull { artist ->
                        val simVal = calculateSimilarity(artist.name, artistName)
                        val notEnoughPop =
                            if (artist.popularity < 10) 0.5 else if (artist.popularity < 20) 0.3 else 0.0
                        simVal - notEnoughPop
                    }
                    if (mostSimilarArtist != null) {
                        Log.d(tag, "Artist found: ${mostSimilarArtist.name}")
                        Pair(mostSimilarArtist, null)
                    } else {
                        Log.d(tag, "Artist not found: $artistName")
                        Pair(null, artistName)
                    }
                }
                is FetchResult.Error -> {
                    Log.e(tag, "Error occurred during searchForSpecificArtists($artistName): ${result.errorData.message}")
                    Pair(null, "$artistName (API Error: ${result.errorData.message})")
                }
                else -> {
                    Log.e(tag, "Unexpected FetchResult state for searchForSpecificArtists($artistName)")
                    Pair(null, "$artistName (Unexpected)")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during searchForSpecificArtists($artistName): ${e.message}")
            return Pair(null, "$artistName (Exception: ${e.message})")
        }
    }

    private fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.lowercase()
        val s2 = str2.lowercase()
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0
        }
        // Use Levenshtein Distance to calculate the edit distance between the two strings.
        val editDistance =
            org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance()
                .apply(s1, s2)

        // Calculate the similarity as 1 - (edit distance / max length of the two strings).
        val maxLength = maxOf(s1.length, s2.length)
        val similarity = if (maxLength > 0) {
            1.0 - (editDistance.toDouble() / maxLength)
        } else {
            1.0 // If both strings are empty, similarity is 1.0.
        }

        return similarity
    }


    private lateinit var responseSimilar: GenerateContentResponse
    private var searchJob: Job? = null
    fun searchSimilarTracksAndArtists(track: String, artist: String) {
        // Check if the search is already in progress, if yes, then return and display initial.
        if (_searchSimilarUiState.value is SearchUiState.Loading) {
            searchJob?.cancel(CancellationException("Cancel search by user."))
            viewModelScope.launch {
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Previous search has successfully cancelled."))
            }
            _searchSimilarUiState.value = SearchUiState.Initial
            return
        }
        Log.d(tag, "Started searchSimilarTracksAndArtists($track, $artist)")
        val startTime = System.currentTimeMillis()
        var geminiFinishedTime = System.currentTimeMillis()

        searchJob = viewModelScope.launch {

            _searchSimilarUiState.value = SearchUiState.Loading

            try {
                val numOfSearch = searchSimilarNum.value
                Log.d(tag, "numOfSearch: $numOfSearch, $searchSimilarNum")
                withContext(Dispatchers.IO) {
                    // Song name and album name for artists list is redundant for now, more precise for future.
                    responseSimilar = GeminiApi().askGemini(
                        """Please list $numOfSearch music tracks of related genres of $track##$artist, where the format mentioned is: Song Name##Artists Name.
                                List only one related music track in each row using format: Song Name##Album Name##Artists Name, while followed by its album and the artists,                               
                                    if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track). 
                                Also, list most related $numOfSearch music artists/band name of the song genre mentioned before, while followed by the famous song name and its album of the artists/band. 
                                List only one related music artist name in each row using format: Artists Name##Song Name##Album Name, responding the name of artists by English, also do not use blank row to separate each artist(only use one row for each artist)
                                Other response rule: Do not use No., and do not respond any other statement, neither.
                                    Use one blank row to separate the response of related music tracks and the response of related music artists."""
                    )
                    Log.d(tag, "response: ${responseSimilar.text}")
                    geminiFinishedTime = System.currentTimeMillis()
                    Log.d(
                        tag,
                        "Gemini response takes time: ${System.currentTimeMillis() - startTime}ms"
                    )
                    responseSimilar.text?.trimIndent()?.let { outputContent ->
                        Log.d(tag, "trimmed response: $outputContent")
                        relatedArtists.clear()
                        relatedTracks.clear()

                        val lines = outputContent.split("\n")
                        val blankLineIndex = lines.indexOf("")

                        if (blankLineIndex != -1) {
                            // First $numOfSearch rows: tracks
                            relatedTracks.addAll(
                                lines.subList(
                                    0,
                                    minOf(numOfSearch, blankLineIndex)
                                )
                            )
                            // Last $numOfSearch rows: artists
                            relatedArtists.addAll(
                                lines.subList(
                                    blankLineIndex + 1,
                                    minOf(blankLineIndex + numOfSearch + 1, lines.size)
                                )
                            )
                        } else {
                            Log.e(tag, "Response format error")
                            //fallback to all tracks
                            relatedTracks.addAll(lines.subList(0, minOf(numOfSearch, lines.size)))
                        }
                        Log.d(tag, "relatedTracks: $relatedTracks")
                        Log.d(tag, "relatedArtists: $relatedArtists")


                        trackTempList.clear()
                        trackNotFoundList.clear()
                        artistTempList.clear()
                        artistNotFoundList.clear()

                        val batchSize = 20
                        // Deal with the batch query of relatedTracks
                        relatedTracks.chunked(batchSize).forEach { trackBatch ->
                            val deferredTrackResults = trackBatch.map { trackInfo ->
                                async { // Inherit Dispatchers.IO
                                    val parts = trackInfo.split("##")
                                    if (parts.size == 3) {
                                        val trackName = parts[0].trim()
                                        val albumName = parts[1].trim()
                                        val artistName = parts[2].trim()
                                        searchForSpecificTrack(trackName, albumName, artistName)
                                    } else {
                                        Log.e(tag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val trackResults = deferredTrackResults.awaitAll()
                            Log.d(tag, "Chunk search finished.")
                            trackResults.forEach { result ->
                                val (track, notFoundId) = result
                                if (track != null) {
                                    trackTempList.add(track)
                                } else if (notFoundId != null) {
                                    trackNotFoundList.add(notFoundId)
                                }
                            }
                        }

                        // Deal with the batch query of relatedArtists
                        relatedArtists.chunked(batchSize).forEach { artistBatch ->
                            val deferredArtistResults = artistBatch.map { artistInfo ->
                                async {
                                    val parts = artistInfo.split("##")
                                    if (parts.size == 3) {
                                        val artistName = parts[0].trim()
                                        // We only need artistName for now
                                        // val trackName = parts[1].trim()
                                        // val albumName = parts[2].trim()
                                        searchForSpecificArtists(artistName)
                                    } else {
                                        Log.e(tag, "Unexpected artist format: $artistInfo")
                                        Pair(null, artistInfo)
                                    }
                                }
                            }
                            // Wait for all artists to be fetched
                            @Suppress("UNCHECKED_CAST")
                            val artistResults = deferredArtistResults.awaitAll()
                            Log.d(tag, "Chunk search finished.")
                            artistResults.forEach { result ->
                                val (artistObj, notFoundId) = result
                                if (artistObj != null) {
                                    artistTempList.add(artistObj)
                                } else if (notFoundId != null) {
                                    artistNotFoundList.add(notFoundId)
                                }
                            }
                        }


                        Log.d(tag, "trackNotFoundList: $trackNotFoundList")
                        Log.d(tag, "artistNotFoundList: $artistNotFoundList")
                        Log.d(tag, "trackTempList: ${trackTempList.joinToString { it.name }}")
                        Log.d(tag, "artistTempList: ${artistTempList.joinToString { it.name }}")

                        // TODO: for albums data
                        val data = SpotifyDataList(
                            trackTempList.toList(),
                            artistTempList.toList(),
                            null,
                            null
                        )
                        _searchSimilarUiState.value = SearchUiState.Success(data)
                        uiEventManager.sendEvent(UiEvent.ShowSnackbar("Search successfully completed."))

                        Log.d(tag, "Tracks and Artists Data: $data")
                    }
                } ?: if (isActive) { // If response.text is null
                    _searchSimilarUiState.value =
                        SearchUiState.Error("Failed to get a valid response from Gemini.")
                } else {
                    Log.d(tag, "Response is null.")
                }
            }
            catch (e: ServerException) {
                if (isActive) {
                    _searchSimilarUiState.value =
                        SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.sendEvent(
                        UiEvent.ShowSnackbarDetail(
                            "Gemini Server Error, please try again later.", e.stackTraceToString()
                        )
                    )
                    Log.d(tag, "Error: $e")
                    e.printStackTrace()
                }
            }
            catch (e: Exception) {
                if (isActive) {
                    _searchSimilarUiState.value =
                        SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.sendEvent(
                        UiEvent.ShowSnackbarDetail(
                            "Some error occurred when searching similar tracks and artists, please try again later.", e.stackTraceToString()
                        )
                    )
                    Log.d(tag, "Error: $e")
                    e.printStackTrace()
                }
            } finally {
                // The "isActive" here is the status of the coroutine that is in finally block
                // If the coroutine is cancelled, isActive will be false in finally block
                // Ensure that this is caused by cancellation and that searchJob is the one that is being cancelled.
                if (_searchSimilarUiState.value is SearchUiState.Loading && !isActive && searchJob?.isCancelled == true) {
                    _searchSimilarUiState.value = SearchUiState.Initial
                    Log.d(
                        tag,
                        "Search was cancelled and UI state reset to Initial in finally."
                    )
                }
            }
            Log.d(tag, "Spotify API takes time: ${System.currentTimeMillis() - geminiFinishedTime}ms")
            Log.d(
                tag,
                "Search job finished. Overall time: ${(System.currentTimeMillis() - startTime)}ms"
            )
        }
    }

    private fun handleApiError(error: ApiError) {
        viewModelScope.launch {
            val uiEvent = globalErrorHandler.processError(error, tag)

            when (uiEvent) {
                is UiEvent.ShowSnackbar -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar(uiEvent.message))
                }
                is UiEvent.ShowSnackbarDetail -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarDetail(uiEvent.message, uiEvent.detail))
                }
                is UiEvent.Navigate -> {
                    uiEventManager.sendEvent(UiEvent.Navigate(uiEvent.route))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar(uiEvent.message))
                    uiEventManager.sendEvent(UiEvent.Navigate(uiEvent.navigationRoute))
                }
            }
        }
    }
}
