package com.example.geminispotifyapp.features.findmusic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle


@Composable
fun FindMusic(
    viewModel: FindMusicViewModel = hiltViewModel()
) {
    val wmo by viewModel.wmo.collectAsStateWithLifecycle()
    val temperature2m by viewModel.temperature2m.collectAsStateWithLifecycle()
    val allForecastTimes by viewModel.allForecastTimes.collectAsStateWithLifecycle()

    FindMusicScreen(wmo, temperature2m, allForecastTimes)
}

@Composable
fun FindMusicScreen(
    wmo: List<Float?>?,
    temperature2m: List<Float?>?,
    allForecastTimes: List<String?>
) {
//    LazyColumn (
//        modifier = Modifier.fillMaxSize(),
//    ) {
//        item {
//            Text(text = "For Test:")
//        }
//        itemsIndexed(wmo ?: emptyList()) { index, wmoValue ->
//            val temp = temperature2m?.getOrNull(index)
//            val time = allForecastTimes.getOrNull(index)
//            Row {
//                Text("$time, Temp: $temp,  WMO: $wmoValue")
//            }
//        }
//    }
}


@Composable
fun ContentScreen(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}