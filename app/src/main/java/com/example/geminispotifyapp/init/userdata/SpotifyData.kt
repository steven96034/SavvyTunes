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

@Composable
fun SpotifyDataScreen(viewModel: SpotifyDataViewModel = hiltViewModel()) {
    val downloadState by viewModel.downLoadState.collectAsState()


    SpotifyDataPage(downloadState, { viewModel.fetchData() }, { viewModel.startAuthentication() })
}

@Composable
private fun SpotifyDataPage(downloadState: DownLoadState, fetchData: () -> Unit, startAuthentication: () -> Unit) {

    LaunchedEffect(Unit) {
        fetchData()
    }

    when (downloadState) {
        DownLoadState.Initial -> {
            Log.d("SpotifyDataScreen", "Initial State")
            InitContent()
        }

        DownLoadState.Loading -> {
            Log.d("SpotifyDataScreen", "Loading State")
            LoadingContent()
        }

        is DownLoadState.Error -> {
//            if (downloadState.message == "ReAuthenticationRequired") {
//                Log.d("SpotifyDataScreen", "ReAuthenticationRequired State")
//                ReAuthenticationRequiredContent(startAuthentication)
//            } else {
                Log.d("SpotifyDataScreen", "Error State")
                ErrorContent(downloadState.message,startAuthentication)
//            }
        }

        else -> { // Success
            val successData = (downloadState as DownLoadState.Success).data
            Log.d("SpotifyDataScreen", "Success Data: $successData")
            MainPage()
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