package com.example.geminispotifyapp.presentation.features.settings.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.data.remote.interceptor.ApiError
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.data.remote.model.UserProfileResponse
import com.example.geminispotifyapp.core.utils.UiEvent
import com.example.geminispotifyapp.core.utils.UiEventManager
import com.example.geminispotifyapp.core.utils.FetchResult
import com.example.geminispotifyapp.presentation.LOGIN_ROUTE
import com.example.geminispotifyapp.core.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val globalErrorHandler: GlobalErrorHandler,
    private val uiEventManager: UiEventManager
) : ViewModel() {

    private val _userProfileState = MutableStateFlow<FetchResult<UserProfileResponse>>(FetchResult.Initial)
    val userProfileState: StateFlow<FetchResult<UserProfileResponse>> = _userProfileState.asStateFlow()

    private val tag = "ProfileViewModel"
    private var hasFetchedOnce = false

    fun fetchUserProfileIfNeeded() {
        if (hasFetchedOnce || _userProfileState.value is FetchResult.Loading) {
            return
        }
        fetchUserProfile()
    }

    fun fetchUserProfile() {
        _userProfileState.value = FetchResult.Loading
        // hasFetchedOnce should be set only on successful fetch
        viewModelScope.launch {
            val result = spotifyRepository.getUserProfile()
            _userProfileState.value = result // Directly assign the result

            when (result) {
                is FetchResult.Success -> {
                    hasFetchedOnce = true
                }
                is FetchResult.Error -> {
                    handleApiError(result.errorData)
                }
                // No need to handle Initial or Loading here as they are covered by the assignment above
                // or are intermediate states from the repository which are less likely.
                else -> Unit // Handle FetchResult.Initial or Loading from repository if necessary, though unlikely path here
            }
        }
    }

    fun onOpenLinkFailed(e: Exception) {
        viewModelScope.launch {
            uiEventManager.sendEvent(UiEvent.ShowSnackbarDetail("Could not open the link. Please make sure Spotify is installed or try again.", e.message  ?: "Unknown error"))
        }
    }

    fun logOut() {
        viewModelScope.launch {
            spotifyRepository.performLogOutAndCleanUp()
            uiEventManager.sendEvent(UiEvent.Navigate(LOGIN_ROUTE))
            uiEventManager.sendEvent(UiEvent.ShowSnackbar("You have logged out."))
        }
    }

    private fun handleApiError(error: ApiError) {
        viewModelScope.launch {
            val uiEvent = globalErrorHandler.processError(error, tag)
            when (uiEvent) {
                is UiEvent.ShowSnackbar -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbar(uiEvent.message))
                }
                is UiEvent.ShowSnackbarDetail -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarDetail(uiEvent.message, uiEvent.detail))
                }
                is UiEvent.Navigate -> {
                    uiEventManager.sendEvent(UiEvent.Navigate(uiEvent.route))
                }
                is UiEvent.ShowSnackbarWithAction -> {
                    uiEventManager.sendEvent(UiEvent.ShowSnackbarWithAction(uiEvent.message, uiEvent.actionLabel))
                }
                is UiEvent.Unauthorized -> {
                    uiEventManager.sendEvent(UiEvent.Unauthorized(uiEvent.message))
                }
                else -> {}
            }
        }
    }
}
