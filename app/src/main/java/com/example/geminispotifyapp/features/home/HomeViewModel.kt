package com.example.geminispotifyapp.features.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SearchUiState
import com.example.geminispotifyapp.SpotifyRepositoryImpl
import com.example.geminispotifyapp.TracksAndArtists
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
    private var _searchSimilarUiState: MutableStateFlow<SearchUiState> = MutableStateFlow(
        SearchUiState.Initial
    )
    val searchSimilarUiState: StateFlow<SearchUiState> = _searchSimilarUiState.asStateFlow()

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


//    fun searchTrack(trackName: String) {
//        viewModelScope.launch {
//            _searchTrackUiState.value = SearchUiState.Loading
//            try {
//                val response = spotifyRepository.searchData("track:$trackName", "track")
//                // 處理前十個結果
//                val artists = response.tracks!!.items
//                    .take(10)
//                    .flatMap { it.artists }
//                    .map { it.name }
//                    .distinct()//移除重複的名稱
//                _searchTrackUiState.value = SearchUiState.Success(artists)
//            } catch (e: Exception) {
//                _searchTrackUiState.value = SearchUiState.Error("Search error")
//            }
//        }
//    }

    fun onTrackInputChange(newTrack: String) {
        _trackInput.value = newTrack
    }

    fun onArtistInputChange(newArtist: String) {
        _artistInput.value = newArtist
    }

    private suspend fun searchForSpecificTrack(trackName: String, artistName: String) {
        try {
            val response = spotifyRepositoryImpl.searchData("track%2520$trackName", "track")

            val foundTrack = response.tracks!!.items.take(20).find { track ->
                track.artists.any { artist ->
                    artist.name.equals(artistName, ignoreCase = true)
                }
            }
            if (foundTrack != null) {
                trackTempList.add(foundTrack)
                Log.d("Gemini", "Track found: ${foundTrack.name}")
            }
            else {
                //find the most closed track name
                val mostSimilarTrack = response.tracks.items.maxByOrNull { track ->
                    val trackNameSimilarity = calculateSimilarity(track.name, trackName)
                    val artistNameSimilarity = track.artists.maxOfOrNull { artist ->
                        calculateSimilarity(artist.name, artistName)
                    } ?: 0.0
                    trackNameSimilarity + artistNameSimilarity
                }
                if (mostSimilarTrack != null) {
                    trackTempList.add(mostSimilarTrack)
                    Log.d("Gemini", "Track not found: $trackName, but similar track found: ${mostSimilarTrack.name}")
                } else {
                    trackNotFoundList.add(trackName)
                    Log.d("Gemini", "Track not found: $trackName")
                }
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun searchForSpecificArtists(artistName: String) {
            try {
                val response = spotifyRepositoryImpl.searchData("artist%2520$artistName", "artist")
                val mostSimilarArtist = response.artists?.items?.maxByOrNull { artist ->
                    calculateSimilarity(artist.name, artistName)
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
                withContext(Dispatchers.IO) {
                    response = GeminiApi().askGemini(
                        """Please list five music tracks of related genres of $track##$artist, where the format mentioned is: Song Name##Artists Name.
                                List only one related music track in each row using format: Song Name##Artist Name,                                
                                    if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track). 
                                Also, list most related five music artists/band name of the song mentioned before. 
                                List only one related music artist name in each row using format: Artists Name, also do not use blank row to separate each artist(only use one row for each artist)
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
                            // First five rows: tracks
                            relatedTracks.addAll(lines.subList(0, minOf(5, blankLineIndex)))
                            // Last five rows: artists
                            relatedArtists.addAll(
                                lines.subList(
                                    blankLineIndex + 1,
                                    minOf(blankLineIndex + 6, lines.size)
                                )
                            )
                        } else {
                            Log.e("Gemini", "Response format error")
                            //fallback to all tracks
                            relatedTracks.addAll(lines.subList(0, minOf(5, lines.size)))
                        }
                        Log.d("Gemini", "relatedTracks: $relatedTracks")
                        Log.d("Gemini", "relatedArtists: $relatedArtists")



                        trackTempList.clear()
                        trackNotFoundList.clear()
                        artistTempList.clear()
                        artistNotFoundList.clear()

                        relatedTracks.forEach { trackInfo ->
                            val parts = trackInfo.split("##")
                            if (parts.size == 2) {
                                val trackName = parts[0].trim()
                                val artistName = parts[1].trim()
                                Log.d("Gemini", "track: $trackName, artist: $artistName")
                                try {
                                    searchForSpecificTrack(trackName, artistName)
                                } catch (e: Exception) {
                                    Log.e("Gemini", "Error searching for specific track: $e")
                                    throw e
                                }
                            } else {
                                Log.e("Gemini", "Unexpected track format: $trackInfo")
                                // Should be handled here.
                            }
                        }

                        relatedArtists.forEach { artist ->
                            Log.d("Gemini", "artist: $artist")
                            try {
                                searchForSpecificArtists(artist)
                            } catch (e: Exception) {
                                Log.e("Gemini", "Error searching for specific artist: $e")
                                throw e
                            }
                        }
                        Log.d("Gemini", "trackNotFoundList: $trackNotFoundList")
                        Log.d("Gemini", "artistNotFoundList: $artistNotFoundList")
                        Log.d("Gemini", "trackTempList: $trackTempList")
                        Log.d("Gemini", "artistTempList: $artistTempList")


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