package com.example.geminispotifyapp

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

// Only for @UserOkHttpClient services
@Singleton
class TokenInterceptor @Inject constructor(
    private val spotifyRepositoryProvider: Provider<SpotifyRepository> // Use Provider to prevent dependency cycle
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val accessToken = spotifyRepositoryProvider.get().getCurrentAccessToken()

        // If there's an Access Token, add it to the request headers
        val authorizedRequest = if (accessToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .build()
        } else {
            originalRequest
        }
        return chain.proceed(authorizedRequest)
    }
}