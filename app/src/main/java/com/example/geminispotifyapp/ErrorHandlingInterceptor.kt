package com.example.geminispotifyapp

import android.util.Log
import com.example.geminispotifyapp.features.SnackbarMessage.ApiErrorMessage
import com.example.geminispotifyapp.features.UiEventManager
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// 1. 定義一個自定義的 API 錯誤類別
// 透過密封類 (sealed class) 來表示不同類型的錯誤，這讓錯誤處理更具體化
sealed class ApiError(val code: Int, override val message: String?) : IOException(message) {
    // 400 Bad Request: 請求格式錯誤
    class BadRequest(message: String?) : ApiError(400, message)
    // 401 Unauthorized: 驗證失敗或 token 過期 (改由 Authenticator 處理)
    class Unauthorized(message: String?) : ApiError(401, message)
    // 403 Forbidden: 權限不足
    class Forbidden(message: String?) : ApiError(403, message)
    // 404 Not Found: 資源未找到
    class NotFound(message: String?) : ApiError(404, message)
    // 429 Too Many Requests: 請求頻率過高
    class TooManyRequests(message: String?, val retryAfter: Int?) : ApiError(429, message)
    // 500 Internal Server Error: 伺服器內部錯誤
    class ServerError(message: String?) : ApiError(500, message)
    // 其他 HTTP 錯誤 (4xx, 5xx 但不在上述範圍內)
    class HttpError(code: Int, message: String?) : ApiError(code, message)
    // 網路連線錯誤 (例如無網路)
    class NetworkConnectionError(message: String?) : ApiError(-1, message)
    // JSON 解析錯誤或其他未知錯誤
    class UnknownError(message: String?) : ApiError(-2, message)
}

// 用於解析 Spotify API 錯誤響應的資料類別
data class SpotifyErrorResponse(
    @SerializedName("error") val error: ErrorDetail?
) {
    data class ErrorDetail(
        @SerializedName("status") val status: Int,
        @SerializedName("message") val message: String
    )
}

// 2. 創建一個自定義的 Interceptor 類別
// 這個 Interceptor 將負責攔截 HTTP 響應，並根據狀態碼拋出對應的 ApiError
@Singleton
class ErrorHandlingInterceptor @Inject constructor(
    private val uiEventManager: UiEventManager
    //private val spotifyRepositoryProvider: Provider<SpotifyRepository> // Use Provider to prevent dependency cycle
) : Interceptor {

//    // 定義一個 SharedFlow 來發送錯誤事件到 ViewModel 或 UI
//    // 這是一個 One-time Event 的範例，UI 會監聽它來顯示 SnackBar
//    private val _apiErrorEvents = MutableSharedFlow<ApiError>()
//    val apiErrorEvents = _apiErrorEvents.asSharedFlow()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val originalResponse: Response

        try {
            originalResponse = chain.proceed(request)
        } catch (e: IOException) {
            // 捕獲網路連線錯誤 (例如無網路、DNS 解析失敗等)
            val networkError = ApiError.NetworkConnectionError(e.message ?: "Network connection error")
//            runBlocking { // 在 Interceptor 中發送事件通常需要處理協程上下文
//                _apiErrorEvents.emit(networkError)
//            }
            emitApiErrorEvent(networkError)
            throw networkError // 重新拋出錯誤以中斷請求
        }

        // 檢查響應狀態碼
        when {
            originalResponse.isSuccessful -> {
                // 2xx 成功響應，直接返回
                return originalResponse
            }
            // 這裡不再處理 401，它由 Authenticator 負責
            // originalResponse.code == 401 -> { ... }
            originalResponse.code == 400 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.BadRequest(errorMessage)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 403 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.Forbidden(errorMessage)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 404 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.NotFound(errorMessage)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code == 429 -> {
                val retryAfter = originalResponse.header("Retry-After")?.toIntOrNull()
                val error = ApiError.TooManyRequests("Too many requests", retryAfter)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
            originalResponse.code in 500..599 -> {
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.ServerError(errorMessage)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
            else -> {
                // 處理其他未預期的 HTTP 錯誤碼
                val errorBody = originalResponse.peekBody(Long.MAX_VALUE).string()
                val errorMessage = parseSpotifyErrorMessage(errorBody)
                val error = ApiError.HttpError(originalResponse.code, errorMessage)
//                runBlocking { _apiErrorEvents.emit(error) }
                emitApiErrorEvent(error)
                throw error
            }
        }
    }

    // 輔助函數：從 Spotify 錯誤響應中解析錯誤訊息
    private fun parseSpotifyErrorMessage(errorBody: String): String? {
        return try {
            // 1. 使用 toMediaTypeOrNull() 創建 MediaType
            val mediaType = "application/json".toMediaTypeOrNull()

            // 2. 使用 toResponseBody() 創建 ResponseBody
            val responseBody = errorBody.toResponseBody(mediaType)

            // 3. 在 responseBodyConverter 中傳遞 emptyArray() 而不是 null
            val converter = GsonConverterFactory.create().responseBodyConverter(
                SpotifyErrorResponse::class.java,
                emptyArray(),
                Retrofit.Builder().baseUrl("http://localhost/").build() // 假 Retrofit 實例
            )
            val errorResponse = converter?.convert(responseBody) as SpotifyErrorResponse?

            if (errorResponse == null) {
                Log.w("ErrorParsing", "Parsed errorResponse is null for body: $errorBody")
                return "Failed to parse error response, please try again later."
            }

            if (errorResponse.error == null) {
                Log.w("ErrorParsing", "Parsed errorResponse.error is null for body: $errorBody")
                return "Error happened：(${errorBody})，。"
            }

            return errorResponse.error.message
        } catch (e: Exception) {
            Log.e("ErrorHandlingInterceptor", "Failed to parse error body: $errorBody", e)
            null
        }
    }

    fun emitApiErrorEvent(error: ApiError) {
        // 將 ApiError 轉換為 UI 可以直接顯示的訊息，然後透過 publisher 發送
        val displayMessage = when (error) {
            is ApiError.NetworkConnectionError -> "網路連線失敗，請檢查您的網路。"
            //is ApiError.Unauthorized -> "登入資訊已過期，請重新登入。"
            is ApiError.Forbidden -> "您沒有足夠權限執行此操作。"
            is ApiError.NotFound -> "找不到請求的資源。"
            is ApiError.TooManyRequests -> "請求過於頻繁，請等待 ${error.retryAfter ?: 5} 秒後重試。"
            is ApiError.ServerError -> "伺服器暫時不可用，請稍後再試。"
            is ApiError.BadRequest -> "請求發生錯誤，請檢查輸入。"
            else -> "發生未知錯誤: ${error.message}"
        }
        // 發送事件
        Log.d("ErrorHandlingInterceptor", "EmitApiErrorEvent in SnackBar(Before): $displayMessage")
        uiEventManager.showSnackbar(ApiErrorMessage(displayMessage))
        Log.d("ErrorHandlingInterceptor", "EmitApiErrorEvent in SnackBar(After): $displayMessage")
    }
}