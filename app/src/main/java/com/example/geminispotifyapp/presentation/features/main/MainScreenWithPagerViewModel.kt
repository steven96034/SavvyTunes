package com.example.geminispotifyapp.presentation.features.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.presentation.MAIN_APP_ROUTE
import com.example.geminispotifyapp.presentation.features.main.findmusic.FindMusicViewModel
import com.example.geminispotifyapp.presentation.features.main.userdatadetail.recentlyplayed.UiPlayHistoryObject
import com.example.geminispotifyapp.presentation.MainScreen
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
    val uiEventManager: UiEventManager
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

    fun navigateToFindMusicWithTrackAndArtist(item: Any, findMusicViewModel: FindMusicViewModel) {
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
            findMusicViewModel.onTrackInputChange(trackName)
            findMusicViewModel.onArtistInputChange(artistName)
            findMusicViewModel.onDataInputChange(albumName) // actually not used

            // Set the selected track and reset similar search state, also triggering the animation of the search button
            findMusicViewModel.onSelectedSuggestedTrackChange(track)

            // Set these flags explicitly to prevent triggering auto-suggestion immediately after setting input
            findMusicViewModel.onHasSelectedTrackAndInputDoesNotChangeSet(true)
            findMusicViewModel.onHasSelectedArtistAndInputDoesNotChangeSet(true)
            findMusicViewModel.onHasSelectedDataAndInputDoesNotChangeSet(true)
            findMusicViewModel.setHasSelectedTrackOfArtistOrAlbumAndInputDoesNotChange(true)
            val targetRoute = MainScreen.FindMusic.route
            val routeWithParam = "$MAIN_APP_ROUTE/$targetRoute"
            uiEventManager.sendEvent(UiEvent.Navigate(routeWithParam))
        }
    }
}