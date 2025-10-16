package com.example.geminispotifyapp

import android.util.Log
import app.cash.turbine.test
import com.example.geminispotifyapp.auth.SpotifyTokenResponse
import com.example.geminispotifyapp.data.local.AppDatabase
import com.example.geminispotifyapp.data.remote.SpotifyApiService
import com.example.geminispotifyapp.data.remote.SpotifyUserApiService
import com.example.geminispotifyapp.features.userdatadetail.ApiExecutionHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
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
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.HttpException
import retrofit2.Response

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
            repository = SpotifyRepositoryImpl(appDatabase, spotifyUserApiService, spotifyApiService, apiExecutionHelper, testScope) // Re-init to collect new flow
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
            coVerify(exactly = 1) { spotifyApiService.refreshAccessToken("valid_refresh_token", any(), any()) }

            // Verify that the new token was saved to the database
            coVerify(exactly = 1) { appDatabase.saveAccessToken("new_access_token") }
            coVerify(exactly = 1) { appDatabase.saveRefreshToken("new_refresh_token") }
        }

        @Test
        fun `getAccessToken WHEN refresh fails with 401 THEN throws ApiError_Unauthorized`() = runTest(testDispatcher) {
            // Arrange
            val expiredTime = System.currentTimeMillis() - 1000
            coEvery { appDatabase.getExpiresAtFlow() } returns flowOf(expiredTime)
            repository = SpotifyRepositoryImpl(appDatabase, spotifyUserApiService, spotifyApiService, apiExecutionHelper, testScope)
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
            repository = SpotifyRepositoryImpl(appDatabase, spotifyUserApiService, spotifyApiService, apiExecutionHelper, testScope)
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

}