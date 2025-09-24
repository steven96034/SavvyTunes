package com.example.geminispotifyapp.features.userdatadetail.toptracks

import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.TopTracksResponse
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetTopTracksUseCase @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper
) {
    suspend operator fun invoke(): FetchResult<TopTrackData> {
        return try {
            coroutineScope {
                apiExecutionHelper.executeApiOperations(
                    operations = {
                        val topTracksDeferredShort = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopTracks(
                                timeRange = "short_term"
                            )
                        }
                        val topTracksDeferredMedium = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopTracks(
                                timeRange = "medium_term"
                            )
                        }
                        val topTracksDeferredLong = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopTracks(
                                timeRange = "long_term"
                            )
                        }
                        // Return a list of Deferred, executeApiOperations will await them
                        listOf(
                            topTracksDeferredShort,
                            topTracksDeferredMedium,
                            topTracksDeferredLong
                        )
                    },
                    // transformSuccess lambda: Transform the List<SpotifyTracksResponse> to TopTrackData
                    transformSuccess = { results ->
                        // Results is a list of Any?, need to cast and extract items
                        val shortTermTracks = (results.getOrNull(0) as? FetchResult.Success<TopTracksResponse>)?.data?.items ?: emptyList()
                        val mediumTermTracks = (results.getOrNull(1) as? FetchResult.Success<TopTracksResponse>)?.data?.items ?: emptyList()
                        val longTermTracks = (results.getOrNull(2) as? FetchResult.Success<TopTracksResponse>)?.data?.items ?: emptyList()

                        TopTrackData(
                            topTracksShort = shortTermTracks,
                            topTracksMedium = mediumTermTracks,
                            topTracksLong = longTermTracks
                        )
                    }
                )
            }
        } catch (e: Exception) {
            FetchResult.Error(ApiError.UnknownError("An unexpected error occurred during fetch: ${e.message}"))
        }
    }
}