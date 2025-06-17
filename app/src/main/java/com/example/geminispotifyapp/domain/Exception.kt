package com.example.geminispotifyapp.domain

class TokenRefreshFailedException(message: String, cause: Throwable? = null) : Exception(message, cause)

class UserReAuthenticationRequiredException(message: String, cause: Throwable? = null) : Exception(message, cause)