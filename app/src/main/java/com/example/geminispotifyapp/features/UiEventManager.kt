package com.example.geminispotifyapp.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UiEventManager @Inject constructor() {
    private val _snackbarEvent = MutableSharedFlow<SnackbarMessage>(extraBufferCapacity = 1)
    val snackbarEvent = _snackbarEvent
        .distinctUntilChanged()
        .filterNotNull()

    private val managerScope = CoroutineScope(Dispatchers.Main.immediate)

    fun showSnackbar(message: SnackbarMessage) {
        managerScope.launch {
            _snackbarEvent.emit(message)
        }
    }
}