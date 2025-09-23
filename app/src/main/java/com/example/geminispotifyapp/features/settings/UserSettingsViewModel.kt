package com.example.geminispotifyapp.features.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository
): ViewModel() {
    val searchSimilarNum: StateFlow<Int> = spotifyRepository.searchSimilarNumFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )

    val userDataNum: StateFlow<Int> = spotifyRepository.userDataNumFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20
        )
    suspend fun setSearchSimilarNum(searchNum: Int) {
        spotifyRepository.setSearchSimilarNum(searchNum)
        Log.d("UserSettingsViewModel", "setSearchSimilarNum called with $searchNum")
    }
    suspend fun setUserDataNum(userDataNum: Int) {
        spotifyRepository.setUserDataNum(userDataNum)
    }
}