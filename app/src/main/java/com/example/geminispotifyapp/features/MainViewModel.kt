package com.example.geminispotifyapp.features

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(uiEventManager: UiEventManager) : ViewModel() {

    val snackbarEvent = uiEventManager.snackbarEvent

}