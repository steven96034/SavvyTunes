package com.example.geminispotifyapp

import android.util.Log
import com.example.geminispotifyapp.features.SnackbarMessage.ApiErrorMessage
import com.example.geminispotifyapp.features.UiEventManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Use a custom sealed class to represent different types of errors, making error handling more precise
sealed class ApiError(val code: Int, override val message: String?) : IOException(message) {
    // 400 Bad Request: Request format error
    class BadRequest(message: String?) : ApiError(400, message)
    // 401 Unauthorized: Authenticate failed or token expired (handled by Authenticator)
    class Unauthorized(message: String?) : ApiError(401, message)
    // 403 Forbidden: Insufficient permission
    class Forbidden(message: String?) : ApiError(403, message)
    // 404 Not Found: Resource not found
    class NotFound(message: String?) : ApiError(404, message)
    // 429 Too Many Requests: Request limit exceeded
    class TooManyRequests(message: String?, val retryAfter: Int?) : ApiError(429, message)
    // 500 Internal Server Error: Server error
    class ServerError(message: String?) : ApiError(500, message)
    // Other HTTP errors (not in the above ranges, but for 4xx or 5xx)
    class HttpError(code: Int, message: String?) : ApiError(code, message)
    // Network connection error (e.g., no network)
    class NetworkConnectionError(message: String?) : ApiError(-1, message)
    // JSON parsing error or other unknown error
    class UnknownError(message: String?) : ApiError(-2, message)
}

// Data class for standard Spotify API errors (error object with status and message)
data class SpotifyStandardErrorWrapper(
    @SerializedName("error") val errorDetail: SpotifyErrorDetail?
) {
    data class SpotifyErrorDetail(
        @SerializedName("status") val status: Int?, // Make them nullable
        @SerializedName("message") val message: String?
    )
}

// Data class for OAuth specific errors (e.g., invalid_grant)
data class SpotifyOAuthError(
    @SerializedName("error") val errorKey: String?, // "invalid_grant", "invalid_client", etc.
    @SerializedName("error_description") val errorDescription: String?
)

// Custom Interceptor to handle HTTP responses and throw corresponding ApiError
@Singleton
class ErrorHandlingInterceptor @Inject constructor(
    private val uiEventManager: UiEventManager,
    private val gson: Gson
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalResponse: Response

        try {
            originalResponse = chain.proceed(request)
        }
        catch (e: IOException) {
            // Strategy 1: Check the type and message of the exception (not always reliable, but common)
            if (e is java.net.SocketException && e.message?.contains("Socket closed", ignoreCase = true) == true ||
                e.message?.contains("CANCEL", ignoreCase = true) == true || // OkHttp 有時會在訊息中包含 "CANCEL"
                e.message?.contains("Socket operation on nonsocket", ignoreCase = true) == true // 有時在取消時出現
            ) {
                // These above mentioned usually indicate that the request was canceled by the user, but not the real problem of network connection
                Log.i("ErrorHandlingInterceptor", "IOException likely due to request cancellation: ${e.message}")
                // Under this scenario, we should rethrow the original IOException to let the ViewModel's coroutine catch the CancellationException
                // Or, if we are sure that this is a cancellation, we should not throw the original IOException and not display the error message to the user
                throw e
            }
            // Catch network connection errors (e.g., no network, DNS resolution failed)
            val networkError = ApiError.NetworkConnectionError(e.message ?: "Network connection error")
            emitApiErrorEvent(networkError)
            throw networkError
        }

        // Check response status code
        when {
            originalResponse.isSuccessful -> {
                // 2xx successful response, just return
                return originalResponse
            }
            // -- Don't handle 401 here, it is handled by AppAuthenticator -- //
            originalResponse.code == 400 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.BadRequest(errorMessage)
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 403 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.Forbidden(errorMessage)
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 404 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.NotFound(errorMessage)
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 429 -> {
                val retryAfter = originalResponse.header("Retry-After")?.toIntOrNull()
                val error = ApiError.TooManyRequests("Too many requests", retryAfter)
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code in 500..599 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.ServerError(errorMessage)
                emitApiErrorEvent(error)
                throw error
            }
            else -> {
                // Handle other unexpected HTTP error codes
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.HttpError(originalResponse.code, errorMessage)
                emitApiErrorEvent(error)
                throw error
            }
        }
    }

    // Helper function to parse error message from Spotify API error response
//    private fun parseSpotifyErrorMessage(errorBody: String): String? {
//        return try {
//            // Just use the injected Gson instance to deserialize
//            val errorResponse = gson.fromJson(errorBody, SpotifyErrorResponse::class.java)
//            errorResponse?.error?.message ?: "An unknown error occurred." // Default message
//        } catch (e: Exception) {
//            // If JSON parsing fails, it might be because the API returned non-JSON format error
//            Log.e("ErrorHandlingInterceptor", "Failed to parse error body: $errorBody", e)
//            // Just return the original body or a generic error message
//            "Failed to parse error message. Raw response: $errorBody"
//        }
//    }
    private fun parseSpotifyErrorMessage(errorBody: String): String {
        // Attempt 1: Try to parse as an OAuth error (e.g., invalid_grant)
        Log.d("ErrorHandlingInterceptor", "Attempting to parse as OAuth error: $errorBody")
        try {
            val oauthError = gson.fromJson(errorBody, SpotifyOAuthError::class.java)
            if (!oauthError.errorKey.isNullOrBlank() && !oauthError.errorDescription.isNullOrBlank()) {
                return "${oauthError.errorKey}: ${oauthError.errorDescription}" // e.g., "invalid_grant: Refresh token revoked"
            }
        } catch (e: Exception) {
            // Not an OAuth error format or failed to parse as such. Log if verbose debugging is needed.
            Log.v("ErrorHandlingInterceptor", "Not an OAuth error or failed to parse as one: $errorBody", e)
        }

        // Attempt 2: Try to parse as a standard Spotify API error
        try {
            val standardError = gson.fromJson(errorBody, SpotifyStandardErrorWrapper::class.java)
            if (standardError?.errorDetail?.message != null && standardError.errorDetail.message.isNotBlank()) {
                return standardError.errorDetail.message
            }
            // If message is blank, but status exists, could add status info
            if (standardError?.errorDetail?.status != null) {
                return "API Error (Status: ${standardError.errorDetail.status}). Raw: $errorBody"
            }
        } catch (e: Exception) {
            // Not a standard API error format or failed to parse as such.
            Log.v("ErrorHandlingInterceptor", "Not a standard API error or failed to parse as one: $errorBody", e)
        }

        // Fallback: If no specific message could be extracted by either parsing attempt
        Log.w("ErrorHandlingInterceptor", "Could not extract a specific error message from known formats. Raw: $errorBody")
        // It's often most useful to return the raw body if specific parsing fails,
        // so the developer/logs show exactly what the server sent.
        return "Server error. Raw response: $errorBody"
    }

    private fun emitApiErrorEvent(error: ApiError) {
        // Transform ApiError to a message that can be displayed in UI, then sent through publisher
        val displayMessage = when (error) {
            is ApiError.NetworkConnectionError -> "網路連線失敗，請檢查您的網路。"
            //is ApiError.Unauthorized -> "登入資訊已過期，請重新登入。" // Handled by AppAuthenticator
            is ApiError.Forbidden -> "您沒有足夠權限執行此操作，嘗試檢查是否為Premium帳戶。"
            is ApiError.NotFound -> "找不到請求的資源。"
            is ApiError.TooManyRequests -> "請求過於頻繁，請等待 ${error.retryAfter ?: 5} 秒後重試。"
            is ApiError.ServerError -> "伺服器暫時不可用，請稍後再試。"
            is ApiError.BadRequest -> "請求發生錯誤，請檢查輸入。"
            else -> "發生未知錯誤: ${error.message}"
        }
        // Emit event
        Log.d("ErrorHandlingInterceptor", "EmitApiErrorEvent in SnackBar: $displayMessage")
        uiEventManager.showSnackbar(ApiErrorMessage(displayMessage))
    }
}