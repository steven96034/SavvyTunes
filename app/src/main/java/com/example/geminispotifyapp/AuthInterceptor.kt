package com.example.geminispotifyapp

import com.example.geminispotifyapp.utils.AuthConstants
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider

class AuthInterceptor @Inject constructor(
    private val spotifyRepositoryProvider: Provider<SpotifyRepository> // Use Provider to prevent dependency cycle
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Check if the request contains the "No-Auth" header (for auth api service)
        if (originalRequest.header(AuthConstants.NO_AUTH_HEADER) != null) {
            // If the request contains the "No-Auth" header, remove it and execute the request without adding the Authorization header
            return chain.proceed(originalRequest.newBuilder().removeHeader(AuthConstants.NO_AUTH_HEADER).build())
        }

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