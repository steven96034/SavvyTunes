package com.example.geminispotifyapp.init.userdata

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.geminispotifyapp.DownLoadState
import com.example.geminispotifyapp.features.MainPage
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SpotifyDataScreen(viewModel: SpotifyDataViewModel = hiltViewModel()) {

    SpotifyDataPage({ viewModel.fetchData() }, viewModel.downLoadState, { viewModel.startAuthentication() })
}

@Composable
private fun SpotifyDataPage(fetchData: () -> Unit , downLoadStateFlow: StateFlow<DownLoadState>, startAuthentication: () -> Unit) {

    val downLoadState by downLoadStateFlow.collectAsState()

    LaunchedEffect(Unit) {
        fetchData()
    }

    when (downLoadState) {
        DownLoadState.Initial -> {
            Log.d("SpotifyDataScreen", "Initial State")
            InitContent()
        }

        DownLoadState.Loading -> {
            Log.d("SpotifyDataScreen", "Loading State")
            LoadingContent()
        }

        is DownLoadState.Error -> {
            val errorData = (downLoadState as DownLoadState.Error).data
            if (errorData.httpStatusCode == 400) { // || errorData.httpStatusCode == 401
                AuthenticationExpiredContent(startAuthentication)
            } else if (errorData.errorMessage == "Network Error") {
                NetworkErrorContent(onRetry = { fetchData() })
            } else {
                ErrorContent(errorData.errorCause?.message, onRetry = { fetchData() })
            }
            Log.d("SpotifyDataScreen", "Error State: $errorData")
        }

        else -> { // Success
            val successData = (downLoadState as DownLoadState.Success).data
            Log.d("SpotifyDataScreen", "Success Data: $successData")
            MainPage(successData)
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        Modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun InitContent() {
    Box(
        Modifier
        .fillMaxSize()
        .padding(4.dp)
    ) {
        Text("Initializing...", modifier = Modifier.align(Alignment.Center))
    }
}