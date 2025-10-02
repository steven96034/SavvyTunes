package com.example.geminispotifyapp.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.data.SpotifyTrack
import com.example.geminispotifyapp.features.home.HomeViewModel
import com.example.geminispotifyapp.features.userdatadetail.recentlyplayed.UiPlayHistoryObject
import com.example.geminispotifyapp.init.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainScreenWithPagerViewModel @Inject constructor(
    spotifyRepository: SpotifyRepository,
    private val uiEventManager: UiEventManager
) : ViewModel() {
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

    fun navigateToHomeWithTrackAndArtist(item: Any, homeViewModel: HomeViewModel) {
        viewModelScope.launch {
            val track = when (item) {
                is SpotifyTrack -> item
                is UiPlayHistoryObject -> item.originalPlayHistory.track
                else -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar("Cannot get track information. Please try another track..."))
                    return@launch
                }
            }

            val trackName = track.name
            val artistName = track.artists.joinToString(", ") { it.name }
            val albumName = track.album.name

            // Update the input fields of HomeViewModel
            homeViewModel.onTrackInputChange(trackName)
            homeViewModel.onArtistInputChange(artistName)
            homeViewModel.onDataInputChange(albumName) // actually not used

            // Set the selected track and reset similar search state, also triggering the animation of the search button
            homeViewModel.onSelectedSuggestedTrackChange(track)

            // Set these flags explicitly to prevent triggering auto-suggestion immediately after setting input
            homeViewModel.onHasSelectedTrackAndInputDoesNotChangeSet(true)
            homeViewModel.onHasSelectedArtistAndInputDoesNotChangeSet(true)
            homeViewModel.onHasSelectedDataAndInputDoesNotChangeSet(true)
            homeViewModel.setHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange(true)

            uiEventManager.sendEvent(UiEvent.Navigate(Screen.Home.route))
        }
    }
}