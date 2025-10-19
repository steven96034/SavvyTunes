package com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed

import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.PlayHistoryObject
import com.example.geminispotifyapp.data.remote.model.RecentlyPlayedResponse
import com.example.geminispotifyapp.core.utils.ApiExecutionHelper
import com.example.geminispotifyapp.core.utils.FetchResult
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