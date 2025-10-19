package com.example.geminispotifyapp

import android.util.Log
import app.cash.turbine.test
import com.example.geminispotifyapp.data.remote.model.SpotifyTokenResponse
import com.example.geminispotifyapp.data.remote.model.TopArtistsResponse
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.api.SpotifyApiService
import com.example.geminispotifyapp.data.remote.api.SpotifyUserApiService
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.data.repository.SpotifyRepositoryImpl
import com.example.geminispotifyapp.core.utils.ApiExecutionHelper
import com.example.geminispotifyapp.core.utils.FetchResultWithEtag
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException


// Use JUnit 5 extension for MockK
@ExperimentalCoroutinesApi
@ExtendWith(MockKExtension::class)
class SpotifyRepositoryImplTest {

    // Use @MockK annotation to automatically create mock objects
    @MockK
    private lateinit var appDatabase: AppDatabase
    @MockK
    private lateinit var spotifyUserApiService: SpotifyUserApiService
    @MockK
    private lateinit var spotifyApiService: SpotifyApiService
    @MockK
    private lateinit var apiExecutionHelper: ApiExecutionHelper

    // Use TestScope and TestDispatcher to control coroutine execution
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // The object to be tested
    private lateinit var repository: SpotifyRepositoryImpl

    @BeforeEach
    fun setUp() {
        // Before each test, set up mock responses for the Flows used in the `init` block
        coEvery { appDatabase.getAccessTokenFlow() } returns flowOf("initial_token")
        coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(System.currentTimeMillis() + 10000)

        // --- Additional setup ---
        // Since these properties are not suspend functions, use `every`
        // We just need to provide a reasonable default value for them
        every { appDatabase.searchSimilarNumFlow } returns flowOf(10) // For example, return Flow<10> by default
        every { appDatabase.getUserDataNumFlow } returns flowOf(20)
        every { appDatabase.checkMarketIfPlayableFlow } returns flowOf("US")
        every { appDatabase.numOfShowCaseSearchFlow } returns flowOf(5)
        every { appDatabase.languageOfShowCaseSearchFlow } returns flowOf("en")
        every { appDatabase.genreOfShowCaseSearchFlow } returns flowOf("pop")
        every { appDatabase.yearOfShowCaseSearchFlow } returns flowOf("2024")

        // Mock all static methods of the Log class
        mockkStatic(Log::class)
        // When Log.e is called anywhere, we tell MockK to do nothing and return 0 (because the return type of Log.e is Int)
        every { Log.e(any(), any<String>()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0 // Handle the version with a Throwable parameter

        // Create the Repository instance, injecting mock objects and the test CoroutineScope
        repository = SpotifyRepositoryImpl(
            appDatabase = appDatabase,
            spotifyUserApiService = spotifyUserApiService,
            spotifyApiService = spotifyApiService,
            apiExecutionHelper = apiExecutionHelper,
            applicationScope = testScope
        )
    }

    @AfterEach
    fun tearDown() {
        // After each test, unmock the Log class. This is a good practice.
        unmockkStatic(Log::class)
    }

    @Nested
    @DisplayName("Access Token Management")
    inner class AccessTokenManagement {

        @Test
        fun `getAccessToken WHEN token is valid and not expired THEN returns cached token`() = runTest(testDispatcher) {
            // Arrange
            // Let the repository's in-memory flow have a valid token
            repository.currentAccessTokenFlow.test {
                // consume the initial value from setup
                awaitItem()

                // Act
                val token = repository.getAccessToken()

                // Assert
                assertEquals("initial_token", token)

                // Verify that no network request was made to refresh the token
                coVerify(exactly = 0) { spotifyApiService.refreshAccessToken(any(), any(), any()) }
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `getAccessToken WHEN token is expired THEN performs refresh and returns new token`() = runTest(testDispatcher) {
            // Arrange
            // Simulate the database having an expired token and a valid refresh token
            val expiredTime = System.currentTimeMillis() - 1000
            coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(expiredTime)
            repository = SpotifyRepositoryImpl(
                appDatabase,
                spotifyUserApiService,
                spotifyApiService,
                apiExecutionHelper,
                testScope
            ) // Re-init to collect new flow
            advanceUntilIdle() // Let the coroutine in init complete
            coEvery { appDatabase.getRefreshToken() } returns "valid_refresh_token"

            // Simulate the refresh API successfully returning a new token
            val refreshedTokenResponse = SpotifyTokenResponse("new_access_token", "bearer", "",3600,  "new_refresh_token")
            coEvery { spotifyApiService.refreshAccessToken(any(), any(), any()) } returns refreshedTokenResponse

            // Mock the token saving functions
            coEvery { appDatabase.saveAccessToken(any()) } returns Unit
            coEvery { appDatabase.saveRefreshToken(any()) } returns Unit
            coEvery { appDatabase.saveExpiresAt(any()) } returns Unit

            // Act
            val token = repository.getAccessToken()

            // Assert
            assertEquals("new_access_token", token)

            // Verify that the refresh API was called once
            coVerify(exactly = 1) { spotifyApiService.refreshAccessToken("refresh_token", any(), any()) }

            // Verify that the new token was saved to the database
            coVerify(exactly = 1) { appDatabase.saveAccessToken("new_access_token") }
            coVerify(exactly = 1) { appDatabase.saveRefreshToken("new_refresh_token") }
        }

        @Test
        fun `getAccessToken WHEN refresh fails with 401 THEN throws ApiError_Unauthorized`() = runTest(testDispatcher) {
            // Arrange
            val expiredTime = System.currentTimeMillis() - 1000
            coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(expiredTime)
            repository = SpotifyRepositoryImpl(
                appDatabase,
                spotifyUserApiService,
                spotifyApiService,
                apiExecutionHelper,
                testScope
            )
            advanceUntilIdle()
            coEvery { appDatabase.getRefreshToken() } returns "invalid_refresh_token"

            // Simulate the API throwing a 401 HttpException
            val httpException = HttpException(Response.error<Any>(401, "".toResponseBody(null)))
            coEvery { spotifyApiService.refreshAccessToken(any(), any(), any()) } throws httpException

            // Act & Assert
            val error = try {
                repository.getAccessToken()
                null // Should not reach here
            } catch (e: Exception) {
                e
            }

            assertInstanceOf(ApiError.Unauthorized::class.java, error)
            assertTrue(error?.message?.contains("HTTP 401") == true)
        }

        @Test
        fun `getAccessToken WHEN concurrent calls are made THEN refresh is only called once`() = runTest(testDispatcher) {
            // Arrange
            val expiredTime = System.currentTimeMillis() - 1000
            coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(expiredTime)
            repository = SpotifyRepositoryImpl(
                appDatabase,
                spotifyUserApiService,
                spotifyApiService,
                apiExecutionHelper,
                testScope
            )
            advanceUntilIdle()
            coEvery { appDatabase.getRefreshToken() } returns "valid_refresh_token"

            val refreshedTokenResponse = SpotifyTokenResponse("new_access_token", "bearer", "",3600,  "new_refresh_token")
            coEvery { spotifyApiService.refreshAccessToken(any(), any(), any()) } returns refreshedTokenResponse
            coEvery { appDatabase.saveAccessToken(any()) } returns Unit
            coEvery { appDatabase.saveRefreshToken(any()) } returns Unit
            coEvery { appDatabase.saveExpiresAt(any()) } returns Unit

            // Act: Launch two coroutines to call getAccessToken concurrently
            val job1 = launch { repository.getAccessToken() }
            val job2 = launch { repository.getAccessToken() }

            // Wait for both jobs to complete
            job1.join()
            job2.join()

            // Assert
            // Verify that even with two calls, the refresh API was only executed once, proving the Mutex works
            coVerify(exactly = 1) { spotifyApiService.refreshAccessToken(any(), any(), any()) }
        }
    }
    @Nested
    @DisplayName("Data Fetching Operations")
    inner class DataFetchingOperations {

        @Test
        fun `getUserTopArtists WHEN token and API call are successful THEN returns Success result`() = runTest(testDispatcher) {
            // Arrange
            // 1. Ensure getAccessToken returns a valid token (already done in setUp)

            // 2. Prepare a fake successful API response
            val fakeSuccessResponse = mockk<TopArtistsResponse>()
            val successResult = FetchResultWithEtag.Success(fakeSuccessResponse, "etag-123")

            // 3. Mock the behavior of apiExecutionHelper
            coEvery {
                apiExecutionHelper.executeEtaggedOperation<TopArtistsResponse, TopArtistsResponse>(any(), any())
            } returns successResult

            // 4. Mock a value for userDataNumFlow, as it's used within getUserTopArtists
            every { appDatabase.getUserDataNumFlow } returns flowOf(15)

            // Act
            val result = repository.getUserTopArtists(
                timeRange = "medium_term",
                limit = 20, // Although the limit will be overridden by the flow's value, passing it is good practice
                offset = 0,
                ifNoneMatch = null
            )

            // Assert
            // 1. Verify the result is of type Success
            assertThat(result).isInstanceOf(FetchResultWithEtag.Success::class.java)
            val successData = (result as FetchResultWithEtag.Success).data
            assertThat(successData).isEqualTo(fakeSuccessResponse)

            // 2. [Correction Point] Verify that apiExecutionHelper.executeEtaggedOperation was called correctly
            coVerify(exactly = 1) {
                apiExecutionHelper.executeEtaggedOperation<TopArtistsResponse, TopArtistsResponse>(
                    operation = any(),
                    transformSuccess = any()
                )
            }

            // (Optional, but more precise verification)
            // If you want to more deeply verify the content passed to the operation lambda, you can do this:
            val slot = slot<suspend () -> Response<TopArtistsResponse>>()
            coVerify(exactly = 1) {
                apiExecutionHelper.executeEtaggedOperation<TopArtistsResponse, TopArtistsResponse>(capture(slot), any())
            }
            // Although we can't execute slot.captured() because it would make a real network request,
            // the capture itself proves that executeEtaggedOperation was called.
            // In this case, the simple any() match above is sufficient.
        }

        @Test
        fun `getUserTopArtists WHEN apiExecutionHelper returns an error THEN returns Error result`() = runTest(testDispatcher) {
            // Arrange
            // 1. Prepare a fake API error
            val apiError = ApiError.NotFound("User not found")
            val errorResult = FetchResultWithEtag.Error(apiError)

            // 2. Mock apiExecutionHelper to return this error
            coEvery {
                apiExecutionHelper.executeEtaggedOperation<TopArtistsResponse, TopArtistsResponse>(any(), any())
            } returns errorResult

            every { appDatabase.getUserDataNumFlow } returns flowOf(15)

            // Act
            val result = repository.getUserTopArtists(timeRange = "short_term", limit = 10, offset = 0)

            // Assert
            // 1. Verify the result is of type Error
            assertThat(result).isInstanceOf(FetchResultWithEtag.Error::class.java)

            // 2. Verify the error content is the one we mocked
            val errorData = (result as FetchResultWithEtag.Error).errorData
            assertThat(errorData).isEqualTo(apiError)
        }

        @Test
        fun `getUserTopArtists WHEN getAccessToken fails THEN returns Error result with Unauthorized`() = runTest(testDispatcher) {
            // Arrange
            // This setup is correct; the goal is to make getAccessToken() throw an exception.
            coEvery { appDatabase.getRefreshToken() } returns null // Cause token refresh to fail
            coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(System.currentTimeMillis() - 1000) // Ensure token is expired
            // Re-initialize the repository to read the new mock flow
            repository = SpotifyRepositoryImpl(
                appDatabase,
                spotifyUserApiService,
                spotifyApiService,
                apiExecutionHelper,
                testScope
            )
            advanceUntilIdle() // Ensure the coroutine in the init block completes

            // Act
            // Execute the function and store its return value
            val result = repository.getUserTopArtists(timeRange = "long_term", limit = 5, offset = 0)

            // Assert
            // 1. Verify that the returned result is of type FetchResultWithEtag.Error
            assertThat(result).isInstanceOf(FetchResultWithEtag.Error::class.java)

            // 2. Cast the result and verify that its internal error property is of type ApiError.Unauthorized
            val errorResult = result as FetchResultWithEtag.Error
            assertThat(errorResult.errorData).isInstanceOf(ApiError.Unauthorized::class.java)

            // 3. (Optional but recommended) Verify the error message is as expected, ensuring it's the error we anticipated.
            assertThat(errorResult.errorData.message).contains("Refresh token not found")
        }

        @Test
        fun `getRecentlyPlayedTracks WHEN API call fails with a generic exception THEN re-throws the exception`() = runTest(testDispatcher) {
            // Arrange
            // 1. Ensure getAccessToken succeeds
            // (already mocked in setUp, no extra setup needed here)

            // 2. Simulate spotifyUserApiService.getRecentlyPlayed throwing a non-ApiError exception
            val networkException = IOException("Network is down")
            coEvery {
                spotifyUserApiService.getRecentlyPlayed(any(), any(), any(), any())
            } throws networkException

            // Act & Assert
            // Verify that when getRecentlyPlayedTracks is called, it re-throws the underlying IOException
            val exception = assertThrows<IOException> {
                repository.getRecentlyPlayedTracks(limit = 50)
            }

            // (Optional) Assert that the exception message is consistent
            assertThat(exception.message).isEqualTo("Network is down")
        }
    }


    @Nested
    @DisplayName("Data Persistence Operations")
    inner class DataPersistenceOperations {

        @Test
        fun `setUserDataNum SHOULD call appDatabase with correct number`() =
            runTest(testDispatcher) {
                // Arrange
                val numberToSet = 25
                // Prepare mock so that the saveGetUserDataNum method can be called
                coEvery { appDatabase.saveGetUserDataNum(any()) } returns Unit

                // Act
                repository.setUserDataNum(numberToSet)

                // Assert
                // Verify that appDatabase.saveGetUserDataNum was called exactly once, with the argument 25
                coVerify(exactly = 1) { appDatabase.saveGetUserDataNum(numberToSet) }
            }

        @Test
        fun `performLogOutAndCleanUp SHOULD call appDatabase logout`() = runTest(testDispatcher) {
            // Arrange
            // Prepare mock so that the logout method can be called
            coEvery { appDatabase.logout() } returns Unit

            // Act
            repository.performLogOutAndCleanUp()

            // Assert
            // Verify that appDatabase.logout was called exactly once
            coVerify(exactly = 1) { appDatabase.logout() }
        }
    }

    @Nested
    @DisplayName("Authorization Flow")
    inner class AuthorizationFlow {

        @Test
        fun `getAccessTokenThruAuth SHOULD delegate call to apiService and return its response`() = runTest(testDispatcher) {
            // Arrange
            // 1. Prepare a fake token response object
            val fakeTokenResponse = mockk<SpotifyTokenResponse>()
            val code = "auth_code"
            val codeVerifier = "code_verifier"

            // 2. Mock the behavior of apiService
            coEvery {
                spotifyApiService.getAccessToken(
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = any(),
                    clientId = any(),
                    codeVerifier = codeVerifier
                )
            } returns fakeTokenResponse

            // Act
            val result = repository.getAccessTokenThruAuth(
                grantType = "authorization_code",
                code = code,
                redirectUri = "uri",
                clientId = "id",
                codeVerifier = codeVerifier
            )

            // Assert
            // 1. Verify that the returned result is the same object returned by apiService
            assertThat(result).isEqualTo(fakeTokenResponse)

            // 2. Verify that the getAccessToken method of apiService was indeed called once with the correct parameters
            coVerify(exactly = 1) {
                spotifyApiService.getAccessToken(
                    grantType = "authorization_code",
                    code = code,
                    redirectUri = any(),
                    clientId = any(),
                    codeVerifier = codeVerifier
                )
            }
        }
    }

    @Nested
    @DisplayName("Helper Logic")
    inner class HelperLogic {

        @Test
        fun `isTokenExpired WHEN expiryTime is in the future THEN returns false`() {
            // Arrange
            val futureTime = System.currentTimeMillis() + 60_000 // 60 seconds from now

            // Act
            val result = repository.isTokenExpired(futureTime)

            // Assert
            assertThat(result).isFalse()
        }

        @Test
        fun `isTokenExpired WHEN expiryTime is in the past THEN returns true`() {
            // Arrange
            val pastTime = System.currentTimeMillis() - 1000 // 1 second ago

            // Act
            val result = repository.isTokenExpired(pastTime)

            // Assert
            assertThat(result).isTrue()
        }

        @Test
        fun `isTokenExpired WHEN expiryTime is null THEN returns true`() {
            // Act
            val result = repository.isTokenExpired(null)

            // Assert
            assertThat(result).isTrue()
        }
    }
}