package com.example.geminispotifyapp.presentation.features.main

import app.cash.turbine.test
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.data.remote.model.PlayContext
import com.example.geminispotifyapp.data.remote.model.PlayHistoryObject
import com.example.geminispotifyapp.data.remote.model.SimplifiedArtist
import com.example.geminispotifyapp.data.remote.model.SpotifyAlbum
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.presentation.MAIN_APP_ROUTE
import com.example.geminispotifyapp.presentation.MainScreen
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.UiPlayHistoryObject
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
class MainScreenWithPagerViewModelTest {

    // Use @RelaxedMockK so we don't have to set up returns for every mock behavior
    @RelaxedMockK
    private lateinit var spotifyRepository: SpotifyRepository

    @RelaxedMockK
    private lateinit var findMusicViewModel: FindMusicViewModel

    // UiEventManager has no dependencies, so create an instance directly for more realistic integration testing
    private lateinit var uiEventManager: UiEventManager
    private lateinit var viewModel: MainScreenWithPagerViewModel

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        uiEventManager = UiEventManager()

        // Mock the flow in the repository
        val marketFlow = MutableStateFlow("TW")
        every { spotifyRepository.checkMarketIfPlayableFlow } returns marketFlow

        viewModel = MainScreenWithPagerViewModel(spotifyRepository, uiEventManager)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("Item Detail State Tests")
    inner class ItemDetailStateTests {
        @Test
        fun `showItemDetail should update selectedItemForDetail with the given item`() = runTest {
            val testItem = "Test String Item"
            viewModel.selectedItemForDetail.test {
                assertThat(awaitItem()).isNull()

                viewModel.showItemDetail(testItem)

                assertThat(awaitItem()).isEqualTo(testItem)
            }
        }

        @Test
        fun `dismissItemDetail should update selectedItemForDetail to null`() = runTest {
            val testItem = "Another Test Item"
            viewModel.showItemDetail(testItem) // Pre-condition

            viewModel.selectedItemForDetail.test {
                assertThat(awaitItem()).isEqualTo(testItem) // Check initial state after setup

                viewModel.dismissItemDetail()

                assertThat(awaitItem()).isNull()
            }
        }
    }


    @Nested
    @DisplayName("Navigation to Find Music Tests")
    inner class NavigationTests {

        private val sampleTrack = SpotifyTrack(
            album = SpotifyAlbum(
                id = "album1", name = "Test Album", images = emptyList(),
                type = "album",
                externalUrls = emptyMap(),
                artists = emptyList(),
                releaseDate = "2023-01-01",
                releaseDatePrecision = "day",
                totalTracks = 10,
                uri = "spotify:album:album1",
                availableMarkets = listOf("US", "TW")
            ),
            artists = listOf(
                SimplifiedArtist(
                    id = "artist1", name = "Artist A",
                    externalUrls = emptyMap(),
                    uri = "spotify:artist:artist1",
                    href = "https://api.spotify.com/v1/artists/artist1"
                ), SimplifiedArtist(
                    id = "artist2", name = "Artist B",
                    externalUrls = emptyMap(),
                    uri = "spotify:artist:artist2",
                    href = "https://api.spotify.com/v1/artists/artist2"
                )
            ),
            id = "track1",
            name = "Test Track",
            uri = "spotify:track:track1",
            availableMarkets = listOf("US", "TW"),
            discNumber = 1,
            durationMs = 180000,
            explicit = false,
            externalIds = emptyMap(),
            externalUrls = emptyMap(),
            href = "https://api.spotify.com/v1/tracks/track1",
            isPlayable = true,
            linkedFrom = emptyMap(),
            restrictions = emptyMap(),
            popularity = 75,
            trackNumber = 1,
            type = "track",
            isLocal = false
        )

        @Test
        fun `navigateToFindMusicWithTrackAndArtist with SpotifyTrack should call viewmodel methods and navigate`() = runTest {
            // Act
            viewModel.navigateToFindMusicWithTrackAndArtist(sampleTrack, findMusicViewModel)
            testDispatcher.scheduler.advanceUntilIdle() // Ensure coroutines in viewModelScope are completed

            // Assert - Verify that the methods of FindMusicViewModel are called correctly
            verify { findMusicViewModel.onTrackInputChange("Test Track") }
            verify { findMusicViewModel.onArtistInputChange("Artist A, Artist B") }
            verify { findMusicViewModel.onDataInputChange("Test Album") }
            verify { findMusicViewModel.onSelectedSuggestedTrackChange(sampleTrack) }
            verify { findMusicViewModel.onHasSelectedTrackAndInputDoesNotChangeSet(true) }
            verify { findMusicViewModel.onHasSelectedArtistAndInputDoesNotChangeSet(true) }
            verify { findMusicViewModel.onHasSelectedDataAndInputDoesNotChangeSet(true) }
            verify { findMusicViewModel.setHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange(true) }

            // Assert - Verify that the navigation event was sent
            val expectedRoute = "$MAIN_APP_ROUTE/${MainScreen.FindMusic.route}"
            uiEventManager.eventFlow.test {
                val event = awaitItem()
                assertThat(event).isInstanceOf(UiEvent.Navigate::class.java)
                assertThat((event as UiEvent.Navigate).route).isEqualTo(expectedRoute)
            }
        }

        @Test
        fun `navigateToFindMusicWithTrackAndArtist with UiPlayHistoryObject should call viewmodel methods and navigate`() = runTest {
            val historyObject = UiPlayHistoryObject(
                originalPlayHistory = PlayHistoryObject(
                    track = sampleTrack,
                    playedAt = "2023-09-01T12:00:00Z",
                    context = PlayContext(
                        type = "album",
                        uri = "spotify:album:album1",
                        externalUrls = emptyMap()
                    )
                ),
                formattedPlayedAtTimeAgo = "some time ago",
                formattedPlayedAtDateTime = "2023/09/01 12:00"
            )

            // Act
            viewModel.navigateToFindMusicWithTrackAndArtist(historyObject, findMusicViewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert - Verify calls to FindMusicViewModel
            verify { findMusicViewModel.onTrackInputChange("Test Track") }
            verify { findMusicViewModel.onArtistInputChange("Artist A, Artist B") }
            verify { findMusicViewModel.onSelectedSuggestedTrackChange(sampleTrack) }

            // Assert - Verify navigation event
            val expectedRoute = "$MAIN_APP_ROUTE/${MainScreen.FindMusic.route}"
            uiEventManager.eventFlow.test {
                val event = awaitItem()
                assertThat(event).isInstanceOf(UiEvent.Navigate::class.java)
                assertThat((event as UiEvent.Navigate).route).isEqualTo(expectedRoute)
            }
        }

        @Test
        fun `navigateToFindMusicWithTrackAndArtist with invalid item type should send a snackbar event`() = runTest {
            val invalidItem = 12345 // Int is not a supported type

            // Act
            viewModel.navigateToFindMusicWithTrackAndArtist(invalidItem, findMusicViewModel)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert - Verify that a Snackbar event was sent
            val expectedMessage = "Cannot get track information. Please try another track..."
            uiEventManager.eventFlow.test {
                val event = awaitItem()
                assertThat(event).isInstanceOf(UiEvent.ShowSnackbar::class.java)
                assertThat((event as UiEvent.ShowSnackbar).message).isEqualTo(expectedMessage)
            }

            // Assert - Verify that no navigation-related calls were made (verify with exactly = 0)
            verify(exactly = 0) { findMusicViewModel.onTrackInputChange(any()) }
            verify(exactly = 0) { findMusicViewModel.onSelectedSuggestedTrackChange(any()) }
        }
    }
}