package com.example.geminispotifyapp.presentation.features.welcome

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geminispotifyapp.core.utils.FetchResultWithEtag
import com.example.geminispotifyapp.data.remote.api.GeminiApi
import com.example.geminispotifyapp.data.remote.model.WelcomeUserPreferenceResponse
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val geminiApi: GeminiApi,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow<WelcomeUiState>(WelcomeUiState.Loading)
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    val isWelcomeFlowCompletedFlow: StateFlow<Boolean> = spotifyRepository.isWelcomeFlowCompletedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val tag = "WelcomeViewModel"

    fun fetchAndProcessUserPreferences() {
        _uiState.value = WelcomeUiState.Loading
        viewModelScope.launch {
            try {
                // 1. Get user's top tracks
                val topTracksResult = spotifyRepository.getUserTopTracks(
                    timeRange = "medium_term", // Or another suitable default
                    limit = 20,
                    offset = 0,
                    ifNoneMatch = null
                )

                if (topTracksResult is FetchResultWithEtag.Success) {
                    val tracks = topTracksResult.data.items.take(20) // Take up to 20 tracks

                    if (tracks.isNotEmpty()) {
                        // 2. Create prompt for Gemini
                        val promptBuilder = StringBuilder()
                        promptBuilder.append("Based on the following Spotify top tracks, suggest the most one preferred music genre, one typical year for these songs, and one language preference. Do not respond with more than one genre, year, or language. Respond only in JSON format with 'genre', 'year', and 'language' fields:\n")
                        tracks.forEachIndexed { index, track ->
                            promptBuilder.append("${index + 1}. Track: ${track.name}, Artist: ${track.artists.joinToString { it.name }}, Album: ${track.album.name}\n")
                        }

                        // 3. Ask Gemini for preferences
                        val geminiResponse = geminiApi.askGeminiForUserPreferences(promptBuilder.toString())

                        // 4. Parse Gemini response and save
                        val textResult = geminiResponse.text?.trimIndent()
                        if (textResult != null) {
                            Log.d(tag, "Gemini Raw Response: $textResult")
                            var jsonContent = textResult.trimIndent()
                            // Though Schema mode usually does not return Markdown (```json), it is a good practice to clean the logic
                            if (jsonContent.startsWith("```")) {
                                jsonContent = jsonContent.replace(Regex("^```json|^```|```$"), "").trim()
                            }
                            val resultObj = gson.fromJson(jsonContent, WelcomeUserPreferenceResponse::class.java)
                            val genre = resultObj.genre
                            val rawYear = resultObj.year
                            val language = resultObj.language

                            val parsedYear: String = if (rawYear.isNotEmpty()) {
                                // Use regex to find the first sequence of four digits
                                val matchResult = Regex("(\\d{4})").find(rawYear)
                                matchResult?.groupValues?.get(1) ?: "" // Extract the captured group (the 4 digits) or default to empty string
                            } else {
                                ""
                            }

                            Log.d(tag, "Parsed Genre: $genre, Year: $parsedYear, Language: $language")

                            if (genre != "") spotifyRepository.setGenreOfShowCaseSearch(genre)
                            if (parsedYear != "") spotifyRepository.setYearOfShowCaseSearch(parsedYear)
                            if (language != "") spotifyRepository.setLanguageOfShowCaseSearch(language)

                            _uiState.value = WelcomeUiState.Success(genre, parsedYear, language)
                        } else {
                            Log.e(tag, "Gemini response text is null.")
                            _uiState.value = WelcomeUiState.Error("Gemini did not return a text response.")
                        }
                    } else {
                        Log.d(tag, "No top tracks found for the user.")
                        _uiState.value = WelcomeUiState.Success("", "", "")
                    }
                } else if (topTracksResult is FetchResultWithEtag.Error) {
                    _uiState.value = WelcomeUiState.Error(topTracksResult.errorData.message ?: "Failed to fetch top tracks.")
                } else {
                    _uiState.value = WelcomeUiState.Error("Unknown error fetching top tracks.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error processing welcome flow", e)
                _uiState.value = WelcomeUiState.Error("An error occurred: ${e.message}")
            }
        }
    }

    fun onNavigateToMainApp() {
        _uiState.value = WelcomeUiState.Loading
        viewModelScope.launch {
            spotifyRepository.setIsWelcomeFlowCompleted(true) // Ensure it's marked as completed
        }
    }
}

sealed class WelcomeUiState {
    object Loading : WelcomeUiState()
    data class Success(val genre: String, val year: String, val language: String) : WelcomeUiState()
    data class Error(val message: String) : WelcomeUiState()
}