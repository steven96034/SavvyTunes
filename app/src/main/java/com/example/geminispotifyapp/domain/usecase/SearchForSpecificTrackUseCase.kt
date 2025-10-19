package com.example.geminispotifyapp.domain.usecase

import android.util.Log
import com.example.geminispotifyapp.core.utils.StringSimilarityCalculator
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.utils.FetchResult
import javax.inject.Inject

// (Pass from Gemini response)
// Here check the equality of track name, album name and artist name, then calculate the most similar track.
// To get more reachable in spotify search, due to the miswrite of album name of the track from Gemini response,
//      we need to just search by the track name and artist name but more selection criteria (TBD).
class SearchForSpecificTrackUseCase @Inject constructor(
    private val spotifyRepository: SpotifyRepository
) {
    val tag = "SearchForSpecificTrackUseCase"
    suspend operator fun invoke(
        trackName: String,
        albumName: String,
        artistName: String,
        ): Pair<SpotifyTrack?, String?> {
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
                                val trackNameSimilarity =
                                    StringSimilarityCalculator.calculateSimilarity(
                                        track.name,
                                        trackName
                                    )
                                val albumNameSimilarity =
                                    StringSimilarityCalculator.calculateSimilarity(
                                        track.album.name,
                                        albumName
                                    )
                                val artistNameSimilarity = track.artists.maxOfOrNull { artist ->
                                    StringSimilarityCalculator.calculateSimilarity(
                                        artist.name,
                                        artistName
                                    )
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
}