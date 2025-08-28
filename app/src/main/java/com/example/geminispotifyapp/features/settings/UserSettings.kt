package com.example.geminispotifyapp.features.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.features.ContentScreen

@Composable
fun UserSettingsScreen(paddingValues: PaddingValues, viewModel: UserSettingsViewModel = hiltViewModel()) {
    ContentScreen("This is the User Settings Screen.")
}