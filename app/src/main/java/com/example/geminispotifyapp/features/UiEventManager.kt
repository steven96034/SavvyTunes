package com.example.geminispotifyapp.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class ShowSnackbarDetail(val message: String, val detail: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ShowSnackbarWithAction(val message: String, val actionLabel: String) : UiEvent()
    data class Unauthorized(val message: String) : UiEvent()
}

@Singleton
class UiEventManager @Inject constructor() {
    private val managerScope = CoroutineScope(Dispatchers.Main.immediate)

    private val _eventFlow = MutableSharedFlow<UiEvent>(
        replay = 1, // Cache last event to prevent lost events on re-subscription
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventFlow = _eventFlow.asSharedFlow()
    //        .distinctUntilChanged()
    //        .filterNotNull()

    // Called by ViewModels
    fun sendEvent(event: UiEvent) {
        managerScope.launch {
            _eventFlow.emit(event)
        }
    }
}