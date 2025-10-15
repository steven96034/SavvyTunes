package com.example.geminispotifyapp

import io.mockk.*
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.inject.Provider


class TokenAuthenticatorTest {

    // 1. Mock all dependencies
    private lateinit var mockRepository: SpotifyRepository
    private lateinit var mockRepositoryProvider: Provider<SpotifyRepository>
    private lateinit var mockResponse: Response
    private lateinit var mockRequest: Request
    private lateinit var mockRoute: Route

    // Instance of the object under test
    private lateinit var authenticator: TokenAuthenticator

    @BeforeEach
    fun setUp() {
        // Initialize all mocks
        mockRepository = mockk()
        mockRepositoryProvider = mockk()
        mockResponse = mockk(relaxed = true) // relaxed = true avoids mocking all methods of Response
        mockRequest = mockk(relaxed = true)
        mockRoute = mockk()

        // Set up the Provider's behavior
        every { mockRepositoryProvider.get() } returns mockRepository
        // Set up the Response's default behavior
        every { mockResponse.request } returns mockRequest

        // Create an instance of the object under test
        authenticator = TokenAuthenticator(mockRepositoryProvider)
    }

    @Test
    fun `authenticate WHEN response code is not 401 THEN returns null`() {
        // Arrange
        // Set the response status code to not be 401
        every { mockResponse.code } returns 403 // e.g., Forbidden

        // Act
        val result = authenticator.authenticate(mockRoute, mockResponse)

        // Assert
        // Verify the result is null, indicating that we give up handling it
        assertNull(result)
        // **Important**: Verify that there was no attempt to get the token
        coVerify(exactly = 0) { mockRepository.getAccessToken() }
    }

    @Test
    fun `authenticate WHEN response is 401 AND token refresh succeeds THEN returns new request with new token`() {
        // Arrange
        val oldToken = "expired_token"
        val newToken = "new_fresh_token"

        // Set the response code to 401
        every { mockResponse.code } returns 401
        // Set the original request to have the old token
        every { mockRequest.header("Authorization") } returns "Bearer $oldToken"

        // **Key**: Set up the repository's suspend function to successfully return a new token
        coEvery { mockRepository.getAccessToken() } returns newToken

        // Mock the chained calls of request.newBuilder()
        val mockRequestBuilder: Request.Builder = mockk(relaxed = true)
        val newRequest: Request = mockk()
        every { mockRequest.newBuilder() } returns mockRequestBuilder
        every { mockRequestBuilder.header(any(), any()) } returns mockRequestBuilder
        every { mockRequestBuilder.build() } returns newRequest

        // Act
        val result = authenticator.authenticate(mockRoute, mockResponse)

        // Assert
        // Verify the result is the new request we created
        assertEquals(newRequest, result)
        // Verify the repository was called to get a new token
        coVerify(exactly = 1) { mockRepository.getAccessToken() }
        // Verify the header method was called with the correct new token
        verify(exactly = 1) { mockRequestBuilder.header("Authorization", "Bearer $newToken") }
    }

    @Test
    fun `authenticate WHEN response is 401 AND getAccessToken throws exception THEN returns null`() {
        // Arrange
        every { mockResponse.code } returns 401
        every { mockRequest.header("Authorization") } returns "Bearer any_token"

        // **Correction**: Mock the repository to throw an exception when refreshing the token
        // Any Exception type will do, IOException is a very common example
        coEvery { mockRepository.getAccessToken() } throws IOException("Refresh token failed")

        // Act
        val result = authenticator.authenticate(mockRoute, mockResponse)

        // Assert
        // Verify the result is null because runCatching caught the exception
        assertNull(result)
        // Verify that it still tried to refresh the token
        coVerify(exactly = 1) { mockRepository.getAccessToken() }
    }

    @Test
    fun `authenticate WHEN response is 401 AND refreshed token is same as old one THEN returns null`() {
        // Arrange
        val sameToken = "same_old_token"
        every { mockResponse.code } returns 401
        every { mockRequest.header("Authorization") } returns "Bearer $sameToken"

        // Mock the repository to return a token that is exactly the same as the old one
        coEvery { mockRepository.getAccessToken() } returns sameToken

        // Act
        val result = authenticator.authenticate(mockRoute, mockResponse)

        // Assert
        // Verify the result is null to avoid an infinite loop
        assertNull(result)
        // Verify that it still tried to refresh the token
        coVerify(exactly = 1) { mockRepository.getAccessToken() }
    }
}