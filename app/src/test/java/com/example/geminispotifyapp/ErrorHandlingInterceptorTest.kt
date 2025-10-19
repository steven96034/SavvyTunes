package com.example.geminispotifyapp

import android.util.Log
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.data.remote.interceptor.ErrorHandlingInterceptor
import com.google.gson.Gson
import io.mockk.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.lang.reflect.Method
import java.net.SocketException

class ErrorHandlingInterceptorTest {

    private lateinit var gson: Gson
    private lateinit var interceptor: ErrorHandlingInterceptor

    // --- Mocks for intercept tests ---
    private lateinit var mockChain: Interceptor.Chain
    private val dummyRequest = Request.Builder().url("https://dummy.url/").build()

    @BeforeEach
    fun setUp() {
        gson = Gson() // Use a real Gson instance
        interceptor = ErrorHandlingInterceptor(gson)
        mockChain = mockk()

        // Mock static Log methods for all tests
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any(), any()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0

        every { Log.w(any<String>(), any<String>()) } returns 0

        // (可选，但推荐) 同时 mock 其他可能用到的版本，以增加测试的健壮性
        every { Log.w(any<String>(), any<Throwable>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any<Throwable>()) } returns 0

        // Common setup for mockChain
        every { mockChain.request() } returns dummyRequest
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    /**
     * Helper function to invoke the private parseSpotifyErrorMessage method using reflection.
     */
    private fun invokeParseErrorMessage(errorBody: String): String {
        val method: Method = ErrorHandlingInterceptor::class.java.getDeclaredMethod("parseSpotifyErrorMessage", String::class.java)
        method.isAccessible = true
        return method.invoke(interceptor, errorBody) as String
    }

    @Nested
    inner class ParseErrorMessageTests {

        @Test
        fun `parseSpotifyErrorMessage with OAuth error JSON returns formatted string`() {
            val json = """{"error":"invalid_grant","error_description":"Refresh token revoked"}"""
            val expected = "invalid_grant: Refresh token revoked"
            val result = invokeParseErrorMessage(json)
            assertEquals(expected, result)
        }

        @Test
        fun `parseSpotifyErrorMessage with standard API error JSON returns message`() {
            val json = """{"error":{"status":403,"message":"You are not allowed"}}"""
            val expected = "You are not allowed"
            val result = invokeParseErrorMessage(json)
            assertEquals(expected, result)
        }

        @Test
        fun `parseSpotifyErrorMessage with malformed JSON returns fallback message`() {
            val malformedJson = """{"error": "invalid"""
            val expected = "Server error. Raw response: $malformedJson"
            val result = invokeParseErrorMessage(malformedJson)
            assertEquals(expected, result)
        }

        @Test
        fun `parseSpotifyErrorMessage with unrecognized JSON structure returns fallback message`() {
            val unknownJson = """{"data":{"info":"some info"}}"""
            val expected = "Server error. Raw response: $unknownJson"
            val result = invokeParseErrorMessage(unknownJson)
            assertEquals(expected, result)
        }

        @Test
        fun `parseSpotifyErrorMessage with empty string returns fallback message`() {
            val emptyBody = ""
            val expected = "Server error. Raw response: "
            val result = invokeParseErrorMessage(emptyBody)
            assertEquals(expected, result)
        }
    }

    @Nested
    inner class InterceptTests {

        @Test
        fun `intercept WHEN chain proceeds successfully THEN returns original response`() {
            val successResponse = createFakeResponse(200)
            every { mockChain.proceed(any()) } returns successResponse

            val result = interceptor.intercept(mockChain)

            assertEquals(successResponse, result)
        }

        @Test
        fun `intercept WHEN chain throws generic IOException THEN throws NetworkConnectionError`() {
            val originalException = IOException("No network")
            every { mockChain.proceed(any()) } throws originalException

            val thrownError = assertThrows<ApiError.NetworkConnectionError> {
                interceptor.intercept(mockChain)
            }

            assertEquals("No network", thrownError.message)
            assertEquals(-1, thrownError.code)
        }


        @Test
        fun `intercept WHEN chain throws cancellation IOException THEN rethrows original exception`() {
            val cancellationException = SocketException("Socket closed")
            every { mockChain.proceed(any()) } throws cancellationException

            val thrownError = assertThrows<SocketException> {
                interceptor.intercept(mockChain)
            }

            // Verify it's the exact same exception instance that was re-thrown
            assertEquals(cancellationException, thrownError)
        }


        @Test
        fun `intercept WHEN response is 400 BadRequest THEN throws ApiError_BadRequest`() {
            val errorBody = """{"error":{"status":400,"message":"Invalid request"}}"""
            val errorResponse = createFakeResponse(400, errorBody)
            every { mockChain.proceed(any()) } returns errorResponse

            val thrownError = assertThrows<ApiError.BadRequest> {
                interceptor.intercept(mockChain)
            }

            assertEquals("Invalid request", thrownError.message)
            assertEquals(400, thrownError.code)
        }

        @Test
        fun `intercept WHEN response is 429 TooManyRequests with header THEN throws ApiError_TooManyRequests with retryAfter`() {
            val errorResponse = createFakeResponse(429, headers = Headers.Builder().add("Retry-After", "60").build())
            every { mockChain.proceed(any()) } returns errorResponse

            val thrownError = assertThrows<ApiError.TooManyRequests> {
                interceptor.intercept(mockChain)
            }

            assertEquals("Too many requests", thrownError.message)
            assertEquals(60, thrownError.retryAfter)
            assertEquals(429, thrownError.code)
        }

        @Test
        fun `intercept WHEN response is 500 ServerError THEN throws ApiError_ServerError`() {
            val errorBody = """{"error":{"status":500,"message":"Internal server problem"}}"""
            val errorResponse = createFakeResponse(500, errorBody)
            every { mockChain.proceed(any()) } returns errorResponse

            val thrownError = assertThrows<ApiError.ServerError> {
                interceptor.intercept(mockChain)
            }

            assertEquals("Internal server problem", thrownError.message)
            assertEquals(500, thrownError.code)
        }

        @Test
        fun `intercept WHEN response is an unhandled error code THEN throws ApiError_HttpError`() {
            val errorBody = "Some other error"
            val errorResponse = createFakeResponse(418, errorBody) // I'm a teapot
            every { mockChain.proceed(any()) } returns errorResponse

            val thrownError = assertThrows<ApiError.HttpError> {
                interceptor.intercept(mockChain)
            }

            assertEquals("Server error. Raw response: $errorBody", thrownError.message)
            assertEquals(418, thrownError.code)
        }

        private fun createFakeResponse(
            code: Int,
            body: String = "",
            headers: Headers = Headers.Builder().build()
        ): Response {
            return Response.Builder()
                .request(dummyRequest)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message("Response message")
                .body(body.toResponseBody("application/json".toMediaTypeOrNull()))
                .headers(headers)
                .build()
        }
    }
}