package com.example.geminispotifyapp.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UserSettingsViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository
): ViewModel() {
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()
    fun updateSearchText(text: String) {
        _searchText.value = text
    }
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
    val checkMarketIfPlayable: StateFlow<String?> = spotifyRepository.checkMarketIfPlayableFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
    suspend fun setSearchSimilarNum(searchNum: Int) {
        spotifyRepository.setSearchSimilarNum(searchNum)
    }
    suspend fun setUserDataNum(userDataNum: Int) {
        spotifyRepository.setUserDataNum(userDataNum)
    }
    suspend fun setCheckMarketIfPlayable(market: String?) {
        spotifyRepository.setCheckMarketIfPlayable(market)
    }

}