package com.example.geminispotifyapp.presentation.features.main.userdatadetail.topartists

import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.TopArtistsResponse
import com.example.geminispotifyapp.core.utils.ApiExecutionHelper
import com.example.geminispotifyapp.core.utils.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetTopArtistsUseCase @Inject constructor(
    // Depends on the interface, not the implementation
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper
    ) {
    suspend operator fun invoke(): FetchResult<TopArtistsData> {
        return try {
            coroutineScope {
                apiExecutionHelper.executeApiOperations(
                    operations = {
                        val topArtistsDeferredShort = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "short_term"
                            )
                        }
                        val topArtistsDeferredMedium = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "medium_term"
                            )
                        }
                        val topTracksDeferredLong = async(Dispatchers.IO) {
                            spotifyRepository.getUserTopArtists(
                                timeRange = "long_term"
                            )
                        }
                        // Return a list of Deferred, executeApiOperations will await them
                        listOf(
                            topArtistsDeferredShort,
                            topArtistsDeferredMedium,
                            topTracksDeferredLong
                        )
                    },
                    // transformSuccess lambda: Transform the List<SpotifyArtistsResponse> to TopArtistData
                    transformSuccess = { results ->
                        // Results is a list of SpotifyArtistsResponse, ensure type conversion is correct
                        val shortTermArtist = (results.getOrNull(0) as? FetchResult.Success<TopArtistsResponse>)?.data?.items ?: emptyList()
                        val mediumTermArtists = (results.getOrNull(1) as? FetchResult.Success<TopArtistsResponse>)?.data?.items ?: emptyList()
                        val longTermArtists = (results.getOrNull(2) as? FetchResult.Success<TopArtistsResponse>)?.data?.items ?: emptyList()

                        TopArtistsData(
                            topArtistsShort = shortTermArtist,
                            topArtistsMedium = mediumTermArtists,
                            topArtistsLong = longTermArtists
                        )
                    }
                )
            }
        }
        catch (e: Exception) {
            FetchResult.Error(ApiError.UnknownError("An unexpected error occurred during fetch: ${e.message}"))
        }
    }
}