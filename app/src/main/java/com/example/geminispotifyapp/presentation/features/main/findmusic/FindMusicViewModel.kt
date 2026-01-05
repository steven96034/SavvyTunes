package com.example.geminispotifyapp.presentation.features.main.findmusic

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.remote.model.TrackInformation
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.data.remote.api.GeminiApi
import com.example.geminispotifyapp.domain.usecase.SearchForSpecificTrackUseCase
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.core.utils.GlobalErrorHandler
import com.example.geminispotifyapp.core.utils.StringSimilarityCalculator.calculateSimilarity
import com.example.geminispotifyapp.data.remote.model.SimilarTracksAndArtistsResponse
import com.example.geminispotifyapp.presentation.MainScreen
import com.google.firebase.ai.type.GenerateContentResponse
//import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.ServerException
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions
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
import com.google.firebase.Firebase
import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class SpotifyDataList(
    val tracks: List<SpotifyTrack>?,
    val artists: List<SpotifyArtist>?,
    val albums: List<SpotifyAlbum>?,
    val trackInformation: List<TrackInformation>?
)

@HiltViewModel
class FindMusicViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val uiEventManager: UiEventManager,
    private val globalErrorHandler: GlobalErrorHandler,
    private val searchForSpecificTrackUseCase: SearchForSpecificTrackUseCase,
    private val geminiApi: GeminiApi,
    private val gson: Gson
) : ViewModel() {
    // For Search State
    private var _searchSimilarUiState: MutableStateFlow<UiState<SpotifyDataList>> =
        MutableStateFlow(UiState.Initial)
    val searchSimilarUiState: StateFlow<UiState<SpotifyDataList>> = _searchSimilarUiState.asStateFlow()

    private var _searchDataUiState: MutableStateFlow<UiState<SpotifyDataList>> =
        MutableStateFlow(UiState.Initial)
    val searchDataUiState: StateFlow<UiState<SpotifyDataList>> = _searchDataUiState.asStateFlow()

    private var _searchByIdUiState: MutableStateFlow<UiState<SpotifyDataList>> =
        MutableStateFlow(UiState.Initial)
    val searchByIdUiState: StateFlow<UiState<SpotifyDataList>> = _searchByIdUiState.asStateFlow()

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

    private val tag = "FindMusicViewModel"

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
        //callPythonBackend()
    }

    fun onSelectedSuggestedTrackChange(track: SpotifyTrack?) {
        _selectedSuggestedTrack.value = track
        _searchSimilarUiState.value = UiState.Initial
        _searchByIdUiState.value = UiState.Initial
        _searchButtonAnimationTrigger.value++
    }

    fun onHasSelectedTrackAndInputDoesNotChangeSet(set: Boolean) {
        _hasSelectedTrackAndInputDoesNotChange.value = set
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

        if (_searchDataUiState.value is UiState.Loading) {
            searchInputJob?.cancel(CancellationException("New search by user."))
            Log.d(tag, "New search by user. Total searchCount so far: $searchCount")
        }

        searchInputJob = viewModelScope.launch {
            delay(debouncePeriod)
            ++searchCount
            _searchDataUiState.value = UiState.Loading
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
                                    _searchDataUiState.value = UiState.Error("No tracks found")
                                    return@launch
                                }
                                val tracks = response.tracks.items
                                _searchDataUiState.value =
                                    UiState.Success((SpotifyDataList(tracks, null, null, null)))
                            }

                            Type.ARTIST -> {
                                if (response.artists == null || response.artists.items.isEmpty()) {
                                    _searchDataUiState.value = UiState.Error("No artists found")
                                    return@launch
                                }
                                val artists = response.artists.items
                                _searchDataUiState.value =
                                    UiState.Success((SpotifyDataList(null, artists, null, null)))
                            }

                            Type.ALBUM -> {
                                if (response.albums == null || response.albums.items.isEmpty()) {
                                    _searchDataUiState.value = UiState.Error("No albums found")
                                    return@launch
                                }
                                val albums = response.albums.items
                                _searchDataUiState.value =
                                    UiState.Success((SpotifyDataList(null, null, albums, null)))
                            }

                            Type.ALLMENTIONED -> {
                                if ((response.tracks == null || response.tracks.items.isEmpty()) &&
                                    (response.artists == null || response.artists.items.isEmpty()) &&
                                    (response.albums == null || response.albums.items.isEmpty())
                                ) {
                                    _searchDataUiState.value = UiState.Error("No data found")
                                    return@launch
                                }
                                val tracks = response.tracks?.items
                                val artists = response.artists?.items
                                val albums = response.albums?.items
                                _searchDataUiState.value =
                                    UiState.Success((SpotifyDataList(tracks, artists, albums, null)))
                            }
                        }
                    }
                    is FetchResult.Error -> {
                        handleApiError(result.errorData)
                        _searchDataUiState.value = UiState.Error(result.errorData.message ?: "Some Error Happened...")
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
                    UiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }


    fun getTopTracksOfArtist(artistId: String) {
        if (artistId.isBlank()) return
        viewModelScope.launch {
            _searchByIdUiState.value = UiState.Loading
            try {
                val result = spotifyRepository.getTopTracksOfArtist(artistId)

                when (result) {
                    is FetchResult.Success -> {
                        val response = result.data
                        if (response.tracks.isEmpty()) {
                            _searchByIdUiState.value = UiState.Error("No tracks found in this return list")
                            uiEventManager.sendEvent(UiEvent.ShowSnackbar("No tracks found from the artist."))
                            return@launch
                        }
                        Log.d(tag, "response: $response")
                        _searchByIdUiState.value =
                            UiState.Success((SpotifyDataList(null, null, null, response.tracks)))
                    }
                    is FetchResult.Error -> {
                        val errorMessage = result.errorData.message ?: "Some Error Happened..."
                        handleApiError(result.errorData)
                        _searchByIdUiState.value = UiState.Error(errorMessage)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when getting top tracks of the artist, please try again later.", e.stackTraceToString()
                    )
                )
                _searchByIdUiState.value = UiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }

    fun getAlbumTracks(albumId: String) {
        if (albumId.isBlank()) return
        viewModelScope.launch {
            _searchByIdUiState.value = UiState.Loading
            try {
                val result = spotifyRepository.getAlbumTracks(albumId)

                when (result) {
                    is FetchResult.Success -> {
                        val response = result.data
                        if (response.tracks.isEmpty()) {
                            _searchByIdUiState.value = UiState.Error("No tracks found in this album")
                            uiEventManager.sendEvent(UiEvent.ShowSnackbar("No tracks found in this album."))
                            return@launch
                        }
                        _searchByIdUiState.value =
                            UiState.Success((SpotifyDataList(null, null, null, response.tracks)))
                    }
                    is FetchResult.Error -> {
                        val errorMessage = result.errorData.message ?: "Some Error Happened..."
                        handleApiError(result.errorData)
                        _searchByIdUiState.value = UiState.Error(errorMessage)
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                uiEventManager.sendEvent(
                    UiEvent.ShowSnackbarDetail(
                        "Some error occurred when getting tracks of the album, please try again later.", e.stackTraceToString()
                    )
                )
                _searchByIdUiState.value = UiState.Error(e.message ?: "Some Error Happened...")
            }
        }
    }

    fun onSetSelectedAlbum(album: SpotifyAlbum?) {
        _selectedAlbum.value = album
    }


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


    private lateinit var responseSimilar: GenerateContentResponse
    private var searchJob: Job? = null
    fun searchSimilarTracksAndArtists(track: String, artist: String) {
        // Check if the search is already in progress, if yes, then return and display initial.
        if (_searchSimilarUiState.value is UiState.Loading) {
            searchJob?.cancel(CancellationException("Cancel search by user."))
            viewModelScope.launch {
                uiEventManager.sendEvent(UiEvent.ShowSnackbar("Previous search has successfully cancelled."))
            }
            _searchSimilarUiState.value = UiState.Initial
            return
        }
        Log.d(tag, "Started searchSimilarTracksAndArtists($track, $artist)")
        val startTime = System.currentTimeMillis()
        var geminiFinishedTime = System.currentTimeMillis()

        searchJob = viewModelScope.launch {

            uiEventManager.sendEvent(UiEvent.ShowSnackbar("You can explore other content in app, we'll inform you when it's ready!"))
            _searchSimilarUiState.value = UiState.Loading

            try {
                val numOfSearch = searchSimilarNum.value
                Log.d(tag, "numOfSearch: $numOfSearch, $searchSimilarNum")
                withContext(Dispatchers.IO) {
                    val prompt = """Rules to respond: 
                             List $numOfSearch related genres of music tracks and $numOfSearch related genres of artists/bands based on the following song data:
                             - Song Name: $track
                             - Artists/Bands Name: $artist
                             
                             Strict Output Requirements:
                             1. Response related music tracks with the song name, its album name and its artists/bands name.
                             2. Response related music artists/bands with artists/bands name and their most famous song name and album name.
                             3. Ensure 'albumName' is accurate.
                             4. 'artists' must be a list of strings (e.g., if a song features someone, list them as separate strings).
                             5. Do NOT include track numbers, album names, or any markdown formatting (like ```json).
                             6. Return the result strictly adhering to the provided JSON schema.
                                    """
                    // Song name and album name for artists list is redundant for now, more precise for future.
                    responseSimilar = geminiApi.askGeminiFindMusic(prompt)
                    geminiFinishedTime = System.currentTimeMillis()
                    Log.d(
                        tag,
                        "Gemini response takes time: ${System.currentTimeMillis() - startTime}ms"
                    )
                    var outputContent =
                        responseSimilar.text?.trimIndent() ?: throw Exception("Failed to get a valid response from Gemini.")
                    // Though Schema mode usually does not return Markdown (```json), it's still a good practice to clean the logic
                    if (outputContent.startsWith("```")) {
                        outputContent = outputContent.replace(Regex("^```json|^```|```$"), "").trim()
                    }
                    Log.d(tag, "trimmed response: $outputContent")

                    val resultObj = gson.fromJson(outputContent, SimilarTracksAndArtistsResponse::class.java)
                    Log.d(tag, "resultObj: $resultObj")

                    val similarTracksByGenre = resultObj.similarTracks
                    val similarArtistsByGenre = resultObj.similarArtists

                    val trackTempList = mutableListOf<SpotifyTrack>()
                    val trackNotFoundList = mutableListOf<String>()
                    val artistTempList = mutableListOf<SpotifyArtist>()
                    val artistNotFoundList = mutableListOf<String>()


                    val batchSize = 20
                    coroutineScope {
                        // Deal with the batch query of relatedTracks
                        similarTracksByGenre.chunked(batchSize).forEach { trackBatch ->
                            val deferredTrackResults = trackBatch.map { trackInfo ->
                                async { // Inherit Dispatchers.IO
                                    //val parts = trackInfo.split("##")
                                    if (trackInfo.trackName.isNotEmpty() && trackInfo.albumName.isNotEmpty() && trackInfo.artists.isNotEmpty()) {
                                        val artists = trackInfo.artists.joinToString(",")
                                        searchForSpecificTrackUseCase(trackInfo.trackName, trackInfo.albumName, artists)
                                    } else {
                                        Log.e(tag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo.toString()) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks in the chunk to be fetched
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

                        // Deal with the batch query of relatedTracksOfEmotion
                        similarArtistsByGenre.chunked(batchSize).forEach { artistsBatch ->
                            val deferredTrackResults = artistsBatch.map { trackInfo ->
                                async { // Inherit Dispatchers.IO
                                    //val parts = trackInfo.split("##")
                                    if (trackInfo.trackName.isNotEmpty() && trackInfo.albumName.isNotEmpty() && trackInfo.artists.isNotEmpty()) {
                                        val artists = trackInfo.artists.joinToString(",")
                                        // We only need artistName for now
                                        //searchForSpecificTrackUseCase(trackInfo.trackName, trackInfo.albumName, artists)
                                        searchForSpecificArtists(artists)
                                    } else {
                                        Log.e(tag, "Unexpected track format: $trackInfo")
                                        Pair(null, trackInfo.toString()) // Format error also consider as not found
                                    }
                                }
                            }
                            // Wait for all tracks in the chunk to be fetched
                            val artistsResult = deferredTrackResults.awaitAll()
                            Log.d(tag, "Chunk search finished.")
                            artistsResult.forEach { result ->
                                val (artist, notFoundId) = result
                                if (artist != null) {
                                    artistTempList.add(artist)
                                } else if (notFoundId != null) {
                                    artistNotFoundList.add(notFoundId)
                                }
                            }
                        }
                    }

                    Log.d(tag, "trackNotFoundList: $trackNotFoundList")
                    Log.d(tag, "artistNotFoundList: $artistNotFoundList")
                    Log.d(tag, "trackTempList: ${trackTempList.joinToString { it.name }}")
                    Log.d(tag, "artistTempList: ${artistTempList.joinToString { it.name }}")

                    val data = SpotifyDataList(
                        trackTempList.toList(),
                        artistTempList.toList(),
                        null,
                        null
                    )

                    _searchSimilarUiState.value = UiState.Success(data)
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction("Search successfully completed.", MainScreen.FindMusic.label))

                    Log.d(tag, "Tracks and Artists Data: $data")
                }
            }
            catch (e: ServerException) {
                if (isActive) {
                    _searchSimilarUiState.value =
                        UiState.Error(e.localizedMessage ?: "Some Error Happened...")
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
                        UiState.Error(e.localizedMessage ?: "Some Error Happened...")
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
                if (_searchSimilarUiState.value is UiState.Loading && !isActive && searchJob?.isCancelled == true) {
                    _searchSimilarUiState.value = UiState.Initial
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
                is UiEvent.ShowSnackbarWithAction -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction(uiEvent.message, uiEvent.actionLabel))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.Unauthorized(uiEvent.message))
                }
                else -> {}
            }
        }
    }

    private fun callPythonBackend() {
        viewModelScope.launch {
            try {
                val functions: FirebaseFunctions = Firebase.functions // Specify the region if not us-central1
                val callable = functions.getHttpsCallable("get_python_secret_data")
                callable.call()
                    .addOnSuccessListener { result ->
                        // Success：result.data includes the response data

                        // Due to the backend Python function returning a dict, it will be parsed as a Map in Kotlin
                        val data = result.data as? Map<String, Any>

                        if (data != null) {
                            // Extract "message" field
                            val message = data["message"] as? String

                            if (message != null) {
                                Log.d("CloudFunction", "Successfully received message: $message")
                            } else {
                                Log.e("CloudFunction", "Returned data format error or missing \"message\" field")
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("CloudFunction", "Failed to call cloud function", exception)
                    }
            } catch (e: Exception) {
                Log.e("CloudFunction", "Error calling cloud function", e)
            }
        }
    }
}
