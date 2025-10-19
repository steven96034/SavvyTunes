package com.example.geminispotifyapp.presentation

import androidx.lifecycle.ViewModel
import com.example.geminispotifyapp.core.utils.UiEventManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(val uiEventManager: UiEventManager) : ViewModel()