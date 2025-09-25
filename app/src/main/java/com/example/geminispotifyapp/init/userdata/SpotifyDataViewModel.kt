package com.example.geminispotifyapp.init.userdata

import androidx.lifecycle.ViewModel
import com.example.geminispotifyapp.SpotifyRepository
import com.example.geminispotifyapp.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SpotifyDataViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val globalErrorHandler: GlobalErrorHandler
) : ViewModel() {

}
