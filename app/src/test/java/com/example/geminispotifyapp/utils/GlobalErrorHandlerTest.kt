package com.example.geminispotifyapp.utils

import android.util.Log
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.core.utils.GlobalErrorHandler
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.core.utils.UiEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions

@ExperimentalCoroutinesApi
class GlobalErrorHandlerTest {

    // 1. Simulate dependencies
    private lateinit var mockSpotifyRepository: SpotifyRepository
    private lateinit var globalErrorHandler: GlobalErrorHandler
    private val testTag = "TEST_HANDLER"

    @BeforeEach
    fun setUp() {
        // 2. Before each test, initialize the mock object and the class under test (@BeforeEach)
        mockSpotifyRepository = mockk()
        globalErrorHandler = GlobalErrorHandler(mockSpotifyRepository)

        // 3. Simulate Android's static Log.d method to make it not throw an exception during testing
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
    }

    @AfterEach
    fun tearDown() {
        // 4. After each test, clear the static mock
        unmockkStatic(Log::class)
    }

    @Test
    fun `processError WHEN error is BadRequest THEN returns correct ShowSnackbarDetail event`() = runTest {
        // Arrange
        val error = ApiError.BadRequest("Invalid Body")
        val expectedEvent = UiEvent.ShowSnackbarDetail("Request format error, please check your input", "BadRequest: Invalid Body")

        // Act
        val result = globalErrorHandler.processError(error, testTag)

        // Assert
        Assertions.assertEquals(expectedEvent, result)
        verify(exactly = 1) { Log.d(testTag, "BadRequest: Invalid Body") }
    }

    @Test
    fun `processError WHEN error is TooManyRequests THEN returns ShowSnackbarDetail with retry-after`() = runTest {
        // Arrange
        val error = ApiError.TooManyRequests("Rate limit exceeded", retryAfter = 45)
        val expectedEvent = UiEvent.ShowSnackbarDetail("Too many requests, please wait 45 seconds.", "TooManyRequests: Rate limit exceeded")

        // Act
        val result = globalErrorHandler.processError(error, testTag)

        // Assert
        Assertions.assertEquals(expectedEvent, result)
    }

    @Test
    fun `processError WHEN error is Unauthorized THEN calls logout and returns Unauthorized event`() = runTest {
        // Arrange
        val error = ApiError.Unauthorized("Token expired")
        val expectedEvent = UiEvent.Unauthorized("Unauthorized, please log in again.")
        // Default behavior of suspend functions in the repository
        coEvery { mockSpotifyRepository.performLogOutAndCleanUp() } returns Unit

        // Act
        val result = globalErrorHandler.processError(error, testTag)

        // Assert
        // 5. Assert that the returned event is correct
        Assertions.assertEquals(expectedEvent, result)

        // 6. Assert side-effect: confirm that the performLogOutAndCleanUp method was called
        coVerify(exactly = 1) { mockSpotifyRepository.performLogOutAndCleanUp() }
        verify(exactly = 1) { Log.d(testTag, "Unauthorized: Token expired") }
    }

    // Test 'Unknown' branch to ensure all unexpected error types are caught

    @Test
    fun `processError WHEN error is unknown THEN returns generic ShowSnackbarDetail event`() = runTest {
        // Arrange
        val error = ApiError.UnknownError("Some other issue")
        val expectedEvent = UiEvent.ShowSnackbarDetail("Unknown error, please try again later.", "UnknownError of ApiError: Some other issue")

        // Act
        val result = globalErrorHandler.processError(error, testTag)

        // Assert
        Assertions.assertEquals(expectedEvent, result)
        verify(exactly = 1) { Log.d(testTag, "UnknownError of ApiError: Some other issue") }
    }
}