package com.example.geminispotifyapp.core.utils

import android.util.Log
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Headers
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ApiExecutionHelperTest {

    private lateinit var apiExecutionHelper: ApiExecutionHelper
    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        apiExecutionHelper = ApiExecutionHelper()
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Nested
    @DisplayName("executeApiOperations Tests")
    inner class ExecuteApiOperationsTests {

        @Test
        fun `when all operations succeed, should return Success with transformed data`() = runTest(testDispatcher) {
            // Arrange
            val operations: suspend CoroutineScope.() -> List<kotlinx.coroutines.Deferred<String>> = {
                listOf(
                    async { "Result1" },
                    async {
                        delay(10) // simulate network delay
                        "Result2"
                    }
                )
            }
            val transformSuccess: (List<String>) -> String = { it.joinToString(",") }

            // Act
            val result = apiExecutionHelper.executeApiOperations(operations, transformSuccess)

            // Assert
            assertThat(result).isInstanceOf(FetchResult.Success::class.java)
            val successResult = result as FetchResult.Success
            assertThat(successResult.data).isEqualTo("Result1,Result2")
        }

        @Test
        fun `when one operation returns an error, should return that first error`() = runTest(testDispatcher) {
            // Arrange
            val apiError = ApiError.HttpError(500, "Server Error")
            val operations: suspend CoroutineScope.() -> List<kotlinx.coroutines.Deferred<FetchResult<*>>> = {
                listOf(
                    async { FetchResult.Success("Success1") },
                    async {
                        delay(10)
                        FetchResult.Error(apiError)
                    },
                    async {
                        delay(20)
                        FetchResult.Success("Success2")
                    }
                )
            }

            // Act
            val result = apiExecutionHelper.executeApiOperations(operations) { /* transform not called */ }

            // Assert
            assertThat(result).isInstanceOf(FetchResult.Error::class.java)
            val errorResult = result as FetchResult.Error
            assertThat(errorResult.errorData).isEqualTo(apiError)
        }

        @Test
        fun `when an operation throws HttpException 401, should return Unauthorized error`() = runTest {
            // Arrange
            val response: Response<Any> = Response.error(401, "".toResponseBody(null))
            val httpException = HttpException(response)
            val operations: suspend CoroutineScope.() -> List<kotlinx.coroutines.Deferred<Any>> = {
                listOf(async { throw httpException })
            }
            // Act
            val result = apiExecutionHelper.executeApiOperations(operations) { /* not called */ }

            // Assert
            assertThat(result).isInstanceOf(FetchResult.Error::class.java)
            val errorData = (result as FetchResult.Error).errorData
            assertThat(errorData).isInstanceOf(ApiError.Unauthorized::class.java)
            assertThat((errorData as ApiError.Unauthorized).message).isEqualTo("Authentication failed")
        }

        @Test
        fun `when an operation throws a generic exception, it should be re-thrown`() = runTest {
            // Arrange
            val ioException = IOException("Network failed")
            val operations: suspend CoroutineScope.() -> List<kotlinx.coroutines.Deferred<Any>> = {
                listOf(async { throw ioException })
            }

            // Act & Assert
            assertThrows<IOException> {
                apiExecutionHelper.executeApiOperations(operations) { /* not called */ }
            }
        }
    }

    @Nested
    @DisplayName("executeEtaggedOperation Tests")
    inner class ExecuteEtaggedOperationTests {

        private val testEtag = "\"some-unique-etag-123\""
        private val headersWithEtag = Headers.Builder().add("ETag", testEtag).build()

        @Test
        fun `when operation returns 200 OK with body, should return Success with data and ETag`() = runTest {
            // Arrange
            val rawData = "Raw Data"
            val successResponse: Response<String> = Response.success(rawData, headersWithEtag)
            val operation = suspend { successResponse }
            val transformSuccess: (String) -> String = { "Transformed: $it" }

            // Act
            val result = apiExecutionHelper.executeEtaggedOperation(operation, transformSuccess)

            // Assert
            assertThat(result).isInstanceOf(FetchResultWithEtag.Success::class.java)
            val successResult = result as FetchResultWithEtag.Success
            assertThat(successResult.data).isEqualTo("Transformed: Raw Data")
            assertThat(successResult.eTag).isEqualTo(testEtag)
        }

        @Test
        fun `when operation returns 200 OK with null body, should return Error`() = runTest {
            // Arrange
            val nullBodyResponse: Response<String?> = Response.success(null, headersWithEtag)
            val operation = suspend { nullBodyResponse }

            // Act
            val result = apiExecutionHelper.executeEtaggedOperation(operation) { /* transform not called */ }

            // Assert
            assertThat(result).isInstanceOf(FetchResultWithEtag.Error::class.java)
            val errorResult = result as FetchResultWithEtag.Error
            assertThat(errorResult.errorData).isInstanceOf(ApiError.UnknownError::class.java)
            assertThat((errorResult.errorData as ApiError.UnknownError).message).isEqualTo("Response body is null")
        }

        @Test
        fun `when operation returns 304 Not Modified, should return NotModified with ETag`() = runTest {
            // Arrange
            // Retrofit's Response.error creates a response with a body, so we use code() to identify it
            // 1. Create a dummy okhttp3.Request
            val request = okhttp3.Request.Builder().url("http://localhost/").build()

            // 2. Use the Builder to create an okhttp3.Response with a Code and Headers
            val rawOkHttpResponse = okhttp3.Response.Builder()
                .code(304)
                .message("Not Modified")
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .request(request)
                .headers(headersWithEtag)
                .body("".toResponseBody(null)) // okhttp3.Response also needs a body
                .build()

            // 3. Pass the rawOkHttpResponse to Retrofit's Response.error
            val notModifiedResponse: Response<String> =
                Response.error("".toResponseBody(null), rawOkHttpResponse)
            val operation = suspend { notModifiedResponse }

            // Act
            val result = apiExecutionHelper.executeEtaggedOperation(operation) { /* not called */ }

            // Assert
            assertThat(result).isInstanceOf(FetchResultWithEtag.NotModified::class.java)
            val notModifiedResult = result as FetchResultWithEtag.NotModified
            assertThat(notModifiedResult.eTag).isEqualTo(testEtag)
        }

        @Test
        fun `when operation returns other error code like 404, should return Error`() = runTest {
            // Arrange
            val errorResponse: Response<String> = Response.error(404, "".toResponseBody(null))
            val operation = suspend { errorResponse }

            // Act
            val result = apiExecutionHelper.executeEtaggedOperation(operation) { /* not called */ }

            // Assert
            assertThat(result).isInstanceOf(FetchResultWithEtag.Error::class.java)
            val errorResult = result as FetchResultWithEtag.Error
            assertThat(errorResult.errorData).isInstanceOf(ApiError.UnknownError::class.java)
            assertThat((errorResult.errorData as ApiError.UnknownError).message).isEqualTo("Response code is not 200 or 304")
        }

        @Test
        fun `when operation throws an exception, it should be re-thrown`() = runTest {
            // Arrange
            val ioException = IOException("Network is down")
            val operation = suspend { throw ioException }

            // Act & Assert
            val thrownException = assertThrows<IOException> {
                apiExecutionHelper.executeEtaggedOperation<Any, Any>(operation) { /* not called */ }
            }
            assertThat(thrownException).isEqualTo(ioException)
        }
    }
}