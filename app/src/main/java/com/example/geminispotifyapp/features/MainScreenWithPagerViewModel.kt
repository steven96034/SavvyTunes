package com.example.geminispotifyapp.features

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
class MainScreenWithPagerViewModel @Inject constructor(spotifyRepository: SpotifyRepository) : ViewModel() {
    private val _selectedItemForDetail = MutableStateFlow<Any?>(null)
    val selectedItemForDetail = _selectedItemForDetail.asStateFlow()

    val checkMarketIfPlayable: StateFlow<String?> = spotifyRepository.checkMarketIfPlayableFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun showItemDetail(item: Any?) {
        _selectedItemForDetail.value = item
    }

    fun dismissItemDetail() {
        _selectedItemForDetail.value = null
    }
}