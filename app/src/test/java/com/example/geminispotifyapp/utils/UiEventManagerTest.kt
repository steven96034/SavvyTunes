package com.example.geminispotifyapp.utils

import app.cash.turbine.test
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class UiEventManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var uiEventManager: UiEventManager

    @BeforeEach
    fun setUp() {
        // UiEventManager internally uses Dispatchers.Main, so we replace it with a test dispatcher
        Dispatchers.setMain(testDispatcher)
        uiEventManager = UiEventManager()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("When sendEvent is called, the event should be emitted to the eventFlow")
    fun sendEvent_emitsEventToFlow() = runTest {
        // Arrange
        val testEvent = UiEvent.Navigate("test/route")

        // Act & Assert
        uiEventManager.eventFlow.test {
            // Act: Send the event
            uiEventManager.sendEvent(testEvent)

            // Assert: Verify that the flow received the same event
            val receivedEvent = awaitItem()
            assertThat(receivedEvent).isEqualTo(testEvent)

            // Ensure there are no other unexpected events
            ensureAllEventsConsumed()
        }
    }

    @Test
    @DisplayName("Given multiple events sent, they should be received in order")
    fun sendMultipleEvents_receivesEventsInOrder() = runTest {
        // Arrange
        val event1 = UiEvent.ShowSnackbar("Message 1")
        val event2 = UiEvent.UpdateAppBarTitle("New Title")

        // Act & Assert
        uiEventManager.eventFlow.test {
            uiEventManager.sendEvent(event1)
            uiEventManager.sendEvent(event2)

            assertThat(awaitItem()).isEqualTo(event1)
            assertThat(awaitItem()).isEqualTo(event2)
        }
    }

    @Test
    @DisplayName("Given replay is 1, a new subscriber should receive the last emitted event")
    fun newSubscriber_receivesLastEventDueToReplayCache() = runTest {
        // Arrange
        val event1 = UiEvent.ShowSnackbar("First message, should be missed")
        val event2 = UiEvent.Navigate("last/route")

        // Act: Send two events first, at this point there are no subscribers
        uiEventManager.sendEvent(event1)
        uiEventManager.sendEvent(event2)

        // Wait for the coroutine sending the event to complete
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert: Now, start subscribing to the flow. Should only receive the last event (because replay = 1)
        uiEventManager.eventFlow.test {
            val receivedEvent = awaitItem()
            assertThat(receivedEvent).isEqualTo(event2)
        }
    }
}