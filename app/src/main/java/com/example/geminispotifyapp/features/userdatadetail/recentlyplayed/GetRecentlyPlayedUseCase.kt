package com.example.geminispotifyapp.features.userdatadetail.recentlyplayed

import com.example.geminispotifyapp.ApiError
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.PlayHistoryObject
import com.example.geminispotifyapp.data.RecentlyPlayedResponse
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper
import com.example.geminispotifyapp.features.userdatadetail.FetchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject

class GetRecentlyPlayedUseCase @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val apiExecutionHelper: ApiExecutionHelper
) {
    suspend operator fun invoke(): FetchResult<List<PlayHistoryObject>> {
        return try {
            apiExecutionHelper.executeApiOperations(
                operations = {
                    val recentlyPlayedDeferred = async(Dispatchers.IO) {
                        spotifyRepository.getRecentlyPlayedTracks()
                    }
                    listOf(recentlyPlayedDeferred)
                },
                transformSuccess = { results ->
                    // results is a List<Any?>, we expect a single RecentlyPlayedResponse
                    (results.firstOrNull() as? FetchResult.Success<RecentlyPlayedResponse>)?.data?.items ?: emptyList()
                }
            )
        } catch (e: Exception) {
            FetchResult.Error(ApiError.UnknownError("An unexpected error occurred during fetch: ${e.message}"))
        }
    }
}