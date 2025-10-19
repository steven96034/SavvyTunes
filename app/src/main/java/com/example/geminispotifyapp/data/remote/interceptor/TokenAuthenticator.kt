package com.example.geminispotifyapp.data.remote.interceptor

import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// Only handle under circumstances: HTTP 401 Unauthorized(Main) or 407 Proxy Authentication Required. (Authentication Failed)
@Singleton
class TokenAuthenticator @Inject constructor(
    private val spotifyRepositoryProvider: Provider<SpotifyRepository> // Use Provider to prevent dependency cycle
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {

        // Ignore HTTP 407 Proxy Authentication Required
        if (response.code != 401) {
            return null
        }

        val originalRequest = response.request
        val originalAuthorizationHeader = originalRequest.header("Authorization")

        // Check if the original request was a token refresh request leading to HTTP 401
        // Notice: Avoid infinite loop if the token refresh request failed itself
        // Just implemented the logic in SpotifyRepository...(Keep it for further maintenance...)
        // if (originalRequest.url.pathSegments.contains("refresh_token")) {
        //     return null
        // }

        // Try to get new access token
        val newAccessToken = runCatching {
            runBlocking { // authenticate method is not suspend function, so we need to call it in runBlocking
                spotifyRepositoryProvider.get().getAccessToken() // the method will handle Mutex and actual refresh
            }
        }.getOrNull()

        // If successfully get new Access Token and different from original, retry the original request
        if (newAccessToken != null && originalAuthorizationHeader != "Bearer $newAccessToken") {
            return originalRequest.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        }

        // If can't get new Access Token (e.g. Refresh Token is also invalid), return null to fail the original request
        return null
    }
}