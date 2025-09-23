package com.example.geminispotifyapp.features.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch


@Composable
fun UserSettingsScreen(
    paddingValues: PaddingValues,
    viewModel: UserSettingsViewModel = hiltViewModel(),
) {
    val searchSimilarNum by viewModel.searchSimilarNum.collectAsStateWithLifecycle()
    val userDataNum by viewModel.userDataNum.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    UserSettingsContent(
        paddingValues = paddingValues,
        searchSimilarNum = searchSimilarNum,
        userDataNum = userDataNum,
        onSearchSimilarNumChange = { newValue -> scope.launch { viewModel.setSearchSimilarNum(newValue) } },
        onUserDataNumChange = { newValue -> scope.launch { viewModel.setUserDataNum(newValue) } }
    )
}

@Composable
fun UserSettingsContent(
    paddingValues: PaddingValues,
    searchSimilarNum: Int,
    userDataNum: Int,
    onSearchSimilarNumChange: (Int) -> Unit,
    onUserDataNumChange: (Int) -> Unit,
) {

    LazyColumn(modifier = Modifier.padding(paddingValues).padding(16.dp)) {
        item {
            Text(text = "Number of similar tracks and artists to search: $searchSimilarNum", modifier = Modifier.padding(bottom = 8.dp))
        }
        item {
            Slider(
                value = searchSimilarNum.toFloat(),
                onValueChange = { newValue ->
                    onSearchSimilarNumChange(newValue.toInt())
                },
                valueRange = 1f..50f,
                steps = 49, // (max - min) - 1
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
        item {
            Text(
                text = "Number of user data to retrieve (Top Tracks/Top Artists/Recently Played): $userDataNum",
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }
        item {
            Slider(
                value = userDataNum.toFloat(),
                onValueChange = { newValue ->
                    onUserDataNumChange(newValue.toInt())
                },
                valueRange = 1f..50f,
                steps = 49,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
@Preview
fun UserSettingsScreenPreview() {
    // You can provide a mock ViewModel or use default values for preview
    UserSettingsContent(
        paddingValues = PaddingValues(0.dp),
        searchSimilarNum = 10,
        userDataNum = 20,
        onSearchSimilarNumChange = {},
        onUserDataNumChange = {}
    )
}