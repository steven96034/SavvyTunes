package com.example.geminispotifyapp.data.remote

import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.utils.AuthConstants
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

interface SpotifyApiService {
    @FormUrlEncoded
    @POST("api/token")
    @Headers("${AuthConstants.NO_AUTH_HEADER}: ${AuthConstants.NO_AUTH_HEADER_VALUE}")
    suspend fun getAccessToken(
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("client_id") clientId: String,
        @Field("code_verifier") codeVerifier: String
    ): SpotifyTokenResponse

    @FormUrlEncoded
    @POST("api/token")
    @Headers("${AuthConstants.NO_AUTH_HEADER}: ${AuthConstants.NO_AUTH_HEADER_VALUE}")
    suspend fun refreshAccessToken(
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String
    ): SpotifyTokenResponse
}