package com.example.geminispotifyapp

import com.example.geminispotifyapp.data.remote.interceptor.TokenInterceptor
import com.example.geminispotifyapp.data.repository.SpotifyRepositoryImpl
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import okhttp3.Interceptor
import okhttp3.Request
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.inject.Provider

class TokenInterceptorTest {

    // 1. Mock all external dependencies
    private lateinit var mockRepository: SpotifyRepositoryImpl
    private lateinit var mockRepositoryProvider: Provider<SpotifyRepository>
    private lateinit var mockChain: Interceptor.Chain

    // Create a base original request
    private lateinit var originalRequest: Request

    // Instance of the class under test
    private lateinit var tokenInterceptor: TokenInterceptor

    @BeforeEach
    fun setUp() {
        // Initialize all mocks
        mockRepository = mockk()
        mockRepositoryProvider = mockk()
        mockChain = mockk(relaxed = true)

        // Create a base request for all tests
        originalRequest = Request.Builder().url("https://api.spotify.com/v1/me").build()

        // Configure the Provider's behavior: when .get() is called, return our mockRepository
        every { mockRepositoryProvider.get() } returns mockRepository

        // Set the default behavior for the chain: when .request() is called, return our original request
        every { mockChain.request() } returns originalRequest

        // Create an instance of the class under test
        tokenInterceptor = TokenInterceptor(mockRepositoryProvider)
    }

    @Test
    fun `intercept WHEN repository has token THEN adds Authorization header to request`() {
        // Arrange
        val fakeToken = "BQD...your_valid_token...xyz"
        // Assume the Repository will return a valid token
        every { mockRepository.getCurrentAccessToken() } returns fakeToken

        // Create a slot to capture the Request object passed to chain.proceed()
        val requestSlot = slot<Request>()
        // Tell mockChain to capture the incoming request when proceed is called
        every { mockChain.proceed(capture(requestSlot)) } returns mockk()

        // Act
        tokenInterceptor.intercept(mockChain)

        // Assert
        // Retrieve the captured request from the slot
        val capturedRequest = requestSlot.captured

        // Verify that the header was added correctly
        assertEquals("Bearer $fakeToken", capturedRequest.header("Authorization"))
        // Verify that the URL remains unchanged
        assertEquals(originalRequest.url, capturedRequest.url)

        // Verify that the repository's method was called exactly once
        verify(exactly = 1) { mockRepository.getCurrentAccessToken() }
    }

    @Test
    fun `intercept WHEN repository has no token THEN proceeds with original request without changes`() {
        // Arrange
        // Assume the Repository returns null
        every { mockRepository.getCurrentAccessToken() } returns null

        val requestSlot = slot<Request>()
        every { mockChain.proceed(capture(requestSlot)) } returns mockk()

        // Act
        tokenInterceptor.intercept(mockChain)

        // Assert
        val capturedRequest = requestSlot.captured

        // Verify that the Authorization header of the captured request is null
        assertNull(capturedRequest.header("Authorization"))

        // The most critical verification: confirm that the passed-on request is the original request object itself, not a new instance
        assertEquals(originalRequest, capturedRequest)

        // Verify that the repository's method was still called
        verify(exactly = 1) { mockRepository.getCurrentAccessToken() }
    }
}