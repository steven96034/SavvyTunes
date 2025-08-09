package com.example.geminispotifyapp.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.TracksAndArtists
import com.example.geminispotifyapp.data.SearchResponse
import com.example.geminispotifyapp.data.SharedData.FIND_SIMILAR_NUM
import com.example.geminispotifyapp.data.SpotifyArtist
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.SnackbarMessage
import com.example.geminispotifyapp.features.UiEventManager
import com.google.ai.client.generativeai.type.GenerateContentResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val spotifyRepositoryImpl: SpotifyRepositoryImpl, private val uiEventManager: UiEventManager) : ViewModel() {
    // For Search State
    private var _searchSimilarUiState: MutableStateFlow<SearchUiState> = MutableStateFlow(SearchUiState.Initial)
    val searchSimilarUiState: StateFlow<SearchUiState> = _searchSimilarUiState.asStateFlow()

    private var _searchDataUiState: MutableStateFlow<SearchUiState> = MutableStateFlow(SearchUiState.Initial)
    val searchDataUiState: StateFlow<SearchUiState> = _searchDataUiState.asStateFlow()

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

    private var searchInputJob: Job? = null
    var searchCount = 0

    fun searchTrack(trackName: String) {

        if (_searchDataUiState.value is SearchUiState.Loading) {
            searchInputJob?.cancel(CancellationException("New search by user."))
            Log.d("Gemini", "New search by user. Total searchCount so far: $searchCount")
//            viewModelScope.launch {
//                uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Previous search has successfully cancelled."))
//            }
//            _searchDataUiState.value = SearchUiState.Initial
        }

        searchInputJob = viewModelScope.launch {
            ++searchCount
            _searchDataUiState.value = SearchUiState.Loading
            try {
                val response = spotifyRepositoryImpl.searchData("track:$trackName", "track", 10)
                if (response.tracks == null || response.tracks.items.isEmpty()) {
                    _searchDataUiState.value = SearchUiState.Error("No tracks found")
                    return@launch
                }
                val tracks = response.tracks.items
                _searchDataUiState.value = SearchUiState.Success((TracksAndArtists(tracks, null)))
            }
            catch (e: CancellationException) {
                Log.d("Gemini", "Search for \"$trackName\" was cancelled: ${e.message}")
                //_searchDataUiState.value = SearchUiState.Initial
            }
            catch (e: Exception) {
                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
                _searchDataUiState.value = SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
            }
        }
    }

    fun searchArtist(artistName: String) {
        viewModelScope.launch {
            _searchDataUiState.value = SearchUiState.Loading
            try {
                val response = spotifyRepositoryImpl.searchData("artist:$artistName", "artist", 10)
                if (response.artists == null || response.artists.items.isEmpty()) {
                    _searchDataUiState.value = SearchUiState.Error("No artists found")
                    return@launch
                }
                // 處理前十個結果
//                val artists = response.tracks!!.items
//                    .take(10)
//                    .flatMap { it.artists }
//                    .map { it.name }
//                    .distinct()//移除重複的名稱
                val artists = response.artists.items
                _searchDataUiState.value = SearchUiState.Success((TracksAndArtists(null, artists)))
            } catch (e: Exception) {
                uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
                _searchDataUiState.value = SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
            }
        }
    }

    fun onTrackInputChange(newTrack: String) {
        _trackInput.value = newTrack
    }

    fun onArtistInputChange(newArtist: String) {
        _artistInput.value = newArtist
    }


    // (Pass from Gemini response)
    // Here check the equality of track name, album name and artist name, then calculate the most similar track.
    // To get more reachable in spotify search, due to the miswrite of album name of the track from Gemini response,
    //      we need to just search by the track name and artist name but more selection criteria (TBD).
    private suspend fun searchForSpecificTrack(trackName: String, albumName: String, artistName: String) {
        lateinit var response: SearchResponse
        try {
            var query: String? = ""
            if (trackName.isNotEmpty()) query += "track:\"$trackName\""
            if (albumName.isNotEmpty()) query += " album:\"$albumName\""
            if (artistName.isNotEmpty()) query += " artist:\"$artistName\""
            Log.d("Gemini", "Query: $query")
            if (query.isNullOrEmpty()) {
                Log.d("Gemini", "Query is empty. No search performed.")
                return
            }
            response = spotifyRepositoryImpl.searchData(query, "track")

            // Explicitly check for null and assign to a local variable
            val tracks = response.tracks
            if (tracks == null || tracks.total == 0) {
                // Handle the case where response.tracks is null
                trackNotFoundList.add("$trackName by $artistName")
                Log.e("Gemini", "response.tracks is null")
                return
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
                trackTempList.add(foundTrack)
                Log.d("Gemini", "Track found: ${foundTrack.name}")
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
                    trackTempList.add(mostSimilarTrack)
                    Log.d(
                        "Gemini",
                        "Track not found: $trackName, but similar track found: ${mostSimilarTrack.name}"
                    )
                }
                else {
                    trackNotFoundList.add("$trackName by $artistName")
                    Log.e("Gemini", "Track not found: $trackName")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    // (Pass from Gemini response)
    // To simply get artists, we only get the artists with enough popularity.
    // (More precise still wait for translation of artist name... some modification of prompt in the future.)
    // (Or TBD for searching the tracks or albums of the artists to get most precise artists.)
    private suspend fun searchForSpecificArtists(artistName: String) {
            try {
                val response = spotifyRepositoryImpl.searchData("artist:\"$artistName\"", "artist")
                val mostSimilarArtist = response.artists?.items?.maxByOrNull { artist ->
                    val simVal = calculateSimilarity(artist.name, artistName)
                    val notEnoughPop = if (artist.popularity < 10) 0.5 else if (artist.popularity < 20) 0.3 else 0.0
                    simVal - notEnoughPop
                }
                if (mostSimilarArtist != null) {
                    artistTempList.add(mostSimilarArtist)
                    Log.d("Gemini", "Artist found: ${mostSimilarArtist.name}")
                } else {
                    artistNotFoundList.add(artistName)
                    Log.d("Gemini", "Artist not found: $artistName")
                }
            } catch (e: Exception) {
                throw e
            }
    }

    private fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.lowercase()
        val s2 = str2.lowercase()
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0
        }
        // Use Levenshtein Distance to calculate the edit distance between the two strings.
        val editDistance = org.apache.commons.text.similarity.LevenshteinDistance.getDefaultInstance().apply(s1, s2)

        // Calculate the similarity as 1 - (edit distance / max length of the two strings).
        val maxLength = maxOf(s1.length, s2.length)
        val similarity = if (maxLength > 0) {
            1.0 - (editDistance.toDouble() / maxLength)
        } else {
            1.0 // If both strings are empty, similarity is 1.0.
        }

        return similarity
    }


    private lateinit var response: GenerateContentResponse
    private var searchJob: Job? = null
    fun searchSimilarTracksAndArtists(track: String, artist: String) {

        // Check if the search is already in progress, if yes, then return and display initial.
        if (_searchSimilarUiState.value is SearchUiState.Loading) {
            searchJob?.cancel(CancellationException("Cancel search by user."))
            viewModelScope.launch {
                uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Previous search has successfully cancelled."))
            }
            _searchSimilarUiState.value = SearchUiState.Initial
            return
        }

        searchJob = viewModelScope.launch {

            _searchSimilarUiState.value = SearchUiState.Loading

            try {
                val numOfSearch = FIND_SIMILAR_NUM
                withContext(Dispatchers.IO) {
                    response = GeminiApi().askGemini(
                        """Please list $numOfSearch music tracks of related genres of $track##$artist, where the format mentioned is: Song Name##Artists Name.
                                List only one related music track in each row using format: Song Name##Album Name##Artists Name, while followed by its album and the artists,                               
                                    if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track). 
                                Also, list most related $numOfSearch music artists/band name of the song genre mentioned before, while followed by the famous song name and its album of the artists/band. 
                                List only one related music artist name in each row using format: Artists Name##Song Name##Album Name, also do not use blank row to separate each artist(only use one row for each artist)
                                Other response rule: Do not use No., and do not respond any other statement, neither.
                                    Use one blank row to separate the response of related music tracks and the response of related music artists."""
                    )
                    Log.d("Gemini", "response: ${response.text}")
                    response.text?.trimIndent()?.let { outputContent ->
                        Log.d("Gemini", "trimmed response: $outputContent")
                        relatedArtists.clear()
                        relatedTracks.clear()

                        val lines = outputContent.split("\n")
                        val blankLineIndex = lines.indexOf("")

                        if (blankLineIndex != -1) {
                            // First $numOfSearch rows: tracks
                            relatedTracks.addAll(lines.subList(0, minOf(numOfSearch, blankLineIndex)))
                            // Last $numOfSearch rows: artists
                            relatedArtists.addAll(
                                lines.subList(
                                    blankLineIndex + 1,
                                    minOf(blankLineIndex + numOfSearch + 1, lines.size)
                                )
                            )
                        } else {
                            Log.e("Gemini", "Response format error")
                            //fallback to all tracks
                            relatedTracks.addAll(lines.subList(0, minOf(numOfSearch, lines.size)))
                        }
                        Log.d("Gemini", "relatedTracks: $relatedTracks")
                        Log.d("Gemini", "relatedArtists: $relatedArtists")



                        trackTempList.clear()
                        trackNotFoundList.clear()
                        artistTempList.clear()
                        artistNotFoundList.clear()

                        relatedTracks.forEach { trackInfo ->
                            val parts = trackInfo.split("##")
                            if (parts.size == 3) {
                                val trackName = parts[0].trim()
                                val albumName = parts[1].trim()
                                val artistName = parts[2].trim()
                                Log.d("Gemini", "track: $trackName, album: $albumName, artist: $artistName")
                                try {
                                    searchForSpecificTrack(trackName, albumName, artistName)
                                } catch (e: Exception) {
                                    Log.e("Gemini", "Error searching for specific track: $e")
                                    throw e
                                }
                            } else {
                                Log.e("Gemini", "Unexpected track format: $trackInfo")
                                // Should be handled here.
                            }
                        }

                        relatedArtists.forEach { trackInfo ->
                            val parts = trackInfo.split("##")
                            if (parts.size == 3) {
                                val artistName = parts[0].trim()
                                val trackName = parts[1].trim()
                                val albumName = parts[2].trim()
                                Log.d("Gemini", "artist: $artistName, track: $trackName, album: $albumName")
                                try {
                                    // TODO: Next Test...
                                    searchForSpecificArtists(artistName)
                                } catch (e: Exception) {
                                    Log.e("Gemini", "Error searching for specific track: $e")
                                    throw e
                                }
                            } else {
                                Log.e("Gemini", "Unexpected track format: $trackInfo")
                                // Should be handled here.
                            }
                        }

                        Log.d("Gemini", "trackNotFoundList: $trackNotFoundList")
                        Log.d("Gemini", "artistNotFoundList: $artistNotFoundList")
                        Log.d("Gemini", "trackTempList: ${trackTempList.joinToString { it.name }}")
                        Log.d("Gemini", "artistTempList: ${artistTempList.joinToString { it.name }}")


                        val data = TracksAndArtists(trackTempList.toList(), artistTempList.toList())
                        _searchSimilarUiState.value = SearchUiState.Success(data)
                        uiEventManager.showSnackbar(SnackbarMessage.TextMessage("Search successfully completed."))
                        Log.d("Gemini", "Tracks and Artists Data: $data")
                    }
                } ?: if(isActive) { // 如果 response.text 是 null
                    _searchSimilarUiState.value =
                        SearchUiState.Error("Failed to get a valid response from Gemini.")
                } else {
                    Log.d("Gemini", "Response is null.")
                }
            }
//            catch (e: CancellationException) {
//                    Log.d("Gemini", "Search coroutine was cancelled.")
//                    _searchSimilarUiState.value = SearchUiState.Initial
//                    throw e
//                }
            catch (e: Exception) {
                if (isActive) {
                    _searchSimilarUiState.value =
                        SearchUiState.Error(e.localizedMessage ?: "Some Error Happened...")
                    uiEventManager.showSnackbar(SnackbarMessage.ExceptionMessage(e))
                    Log.d("Gemini", "Error: $e")
                    e.printStackTrace()
                }
            } finally {
                // The "isActive" here is the status of the coroutine that is in finally block
                // If the coroutine is cancelled, isActive will be false in finally block
                // Ensure that this is caused by cancellation and that searchJob is the one that is being cancelled.
                if (_searchSimilarUiState.value is SearchUiState.Loading && !isActive && searchJob?.isCancelled == true) {
                    _searchSimilarUiState.value = SearchUiState.Initial
                    Log.d("Gemini", "Search was cancelled and UI state reset to Initial in finally.")
                }
            }
            Log.d("Gemini", "Search job finished.")
        }
    }


    // For further development.
    /*
                    val response = generativeModel.generateContent(
                    content {
                        text("""請推薦5個與 $artist 風格相似的音樂家或樂團。
                            請只列出名字，每行一個，不要加入編號或其他說明。""")
                    }
                )
                text("""Please list the music genres of $artist.
                            List only one genre in each row, and do not use No. or other statement.""")
 */

    //    // For Spotify API

    //    private var spotifyApi: SpotifyClientApi? = null
//    private var playlist: Playlist? = null

//    fun setSpotifyApi(api: SpotifyClientApi) {
//        spotifyApi = api
//    }
//
//    fun createAndAddTrackToPlaylist(artist: String) {
//        createPrivatePlaylist(artist)
//        addTracksToPlaylist(topTracks.value)
//    }
//
//    private fun fetchTopTracks(artists: List<String>) {
//        viewModelScope.launch {
//            val tracks = mutableListOf<Track>()
//            spotifyApi?.let { api ->
//                artists.forEach { artist ->
//                    val artistResult = api.search.searchArtist(artist).firstOrNull()
//                    artistResult?.id?.let { artistId ->
//                        val topTracks = api.artists.getArtistTopTracks(artistId)
//                        tracks.addAll(topTracks.take(3)) // 取前 3 首熱門歌曲
//                    }
//                }
//            }
//            _topTracks.value = tracks
//        }
//    }
//
//    private fun createPrivatePlaylist(artist: String) {
//        viewModelScope.launch {
//            spotifyApi?.let { api ->
//                val userId = api.users.getClientProfile().id
//                playlist = api.playlists.createClientPlaylist(
//                    name = "Similar Artists Playlist of $artist",
//                    public = false,
//                    description = "Created by MusicApp",
//                    user = userId
//                )
//            }
//        }
//    }
//
//    private fun addTracksToPlaylist(tracks: List<Track>) {
//        viewModelScope.launch {
//            spotifyApi?.let { api ->
//                playlist?.id?.let { playlistId ->
//                    val trackUris = tracks.map { it.uri }
//                    api.playlists.addPlayablesToClientPlaylist(playlistId, *trackUris.toTypedArray())
//                }
//            }
//        }
//    }
}

//class HomePageViewModelFactory(private val spotifyRepository: SpotifyRepository) : ViewModelProvider.Factory {
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(HomePageViewModel::class.java)) {
//            @Suppress("Unchecked_cast")
//            return HomePageViewModel(spotifyRepository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//
//    }
//}