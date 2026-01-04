package com.example.geminispotifyapp.domain.usecase

import android.util.Log
import com.example.geminispotifyapp.core.utils.UiState
import com.example.geminispotifyapp.data.remote.api.GeminiApi
import com.example.geminispotifyapp.data.remote.model.RecommendationResponse
import com.example.geminispotifyapp.data.remote.model.SpotifyTrack
import com.example.geminispotifyapp.data.repository.WeatherResponse
import com.example.geminispotifyapp.domain.repository.SpotifyRepository
import com.example.geminispotifyapp.presentation.features.main.home.TwoTracksList
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import javax.inject.Inject
import kotlin.collections.chunked
import kotlin.collections.map

class FindWeatherRelatedMusic @Inject constructor(
    private val spotifyRepository: SpotifyRepository,
    private val geminiApi: GeminiApi,
    private val searchForSpecificTrackUseCase: SearchForSpecificTrackUseCase,
    private val gson: Gson
) {
    operator fun invoke(weatherResponse: WeatherResponse): Flow<UiState<TwoTracksList>> = flow {
        val musicTag = "WeatherMusic"

        // 1. Emit Loading state immediately at the start
        emit(UiState.Loading)

        try {
            val currentJsonWeather = weatherResponse.current

            val numOfShowCaseSearch = spotifyRepository.numOfShowCaseSearchFlow.first()
            val languageOfShowCaseSearch = spotifyRepository.languageOfShowCaseSearchFlow.first()
            val genreOfShowCaseSearch = spotifyRepository.genreOfShowCaseSearchFlow.first()
            val yearOfShowCaseSearch = spotifyRepository.yearOfShowCaseSearchFlow.first()
            val isRandomYearOfShowCaseSelection = spotifyRepository.isRandomYearOfShowCaseSelectionFlow.first()

            // For random year search in small range
            val randomNumber = (-5..5).random()
            var yearOfSearch = 0
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            if (yearOfShowCaseSearch != "") {
                yearOfSearch = if (isRandomYearOfShowCaseSelection) {
                    val newYear = yearOfShowCaseSearch.toInt() + randomNumber
                    Log.d(musicTag, "yearOfSearch: $newYear (After added by random number: $randomNumber)")
                    if (newYear > currentYear) currentYear else newYear
                } else {
                    yearOfShowCaseSearch.toInt()
                }
            }

            Log.d(musicTag, "numOfShowCaseSearch: $numOfShowCaseSearch, languageOfShowCaseSearch: $languageOfShowCaseSearch, genreOfShowCaseSearch: $genreOfShowCaseSearch, yearOfShowCaseSearch: $yearOfShowCaseSearch")

            val numOfQuery = "$numOfShowCaseSearch"
            val languageOfQuery = if (languageOfShowCaseSearch != "") ", where the tracks should be written in $languageOfShowCaseSearch," else ", where the tracks should be written/included in a great variety of languages,"
            val genreOfQuery = if (genreOfShowCaseSearch != "") " (with genre $genreOfShowCaseSearch)" else ""
            val yearOfQuery = if (yearOfShowCaseSearch != "") " around A.D. $yearOfSearch" else ""

            val wmo = currentJsonWeather.weatherCode
            val temperature = currentJsonWeather.temperature.toFloat()
            val time = currentJsonWeather.time
            // Set two random moods here
            val moodList = listOf("happy", "sad", "energetic", "romantic", "melancholic", "nostalgic", "uplifting", "chill", "motivational", "reflective", "angry")
            val mood = moodList.shuffled().take(2).joinToString(" or ")
            Log.d(musicTag, "mood: $mood")

            val prompt = """Rules to respond: 
                             Recommend related music tracks of songs based on the following user preferences:
                             - Genres: $genreOfQuery
                             - Year: Around $yearOfQuery
                             - Language: $languageOfQuery
                             - Mood: $mood

                             Also, below is the main query of weather condition:
                             The current weather represented in WMO weather interpretation code is $wmo, the current temperature is $temperature, and current time is $time.
                             Please recommend $numOfQuery related music tracks of aforementioned weather condition.
                             Also, recommend $numOfQuery related music tracks of the related emotion of this weather and current time.
                             
                             Strict Output Requirements:
                             1. Select songs that perfectly match the weather condition, genre and language, others are minor factors that should be considered.
                             2. Ensure 'albumName' is accurate.
                             3. 'artists' must be a list of strings (e.g., if a song features someone, list them as separate strings).
                             4. Do NOT include track numbers, album names, or any markdown formatting (like ```json).
                             5. Return the result strictly adhering to the provided JSON schema.
                                    """
            """ Old prompt:
                Rules to respond: List only one related music of track$genreOfQuery$yearOfQuery in each row$languageOfQuery using format: Song Name##Album Name##Artists Name, while followed by its album and the artists,
                             if there is more than one artist, just separate them with comma, also do not use blank row to separate each track(only use one row for each track).
                             Use one blank row to separate the response of aforementioned weather condition and the response of related emotion of this weather.
                             Other response rule: Do not use No., and do not respond any other statement, neither.

                             Below is the main query:
                             The current weather represented in WMO weather interpretation code is $wmo, the current temperature is $temperature, and current time is $time.
                             Please recommend $numOfQuery related music tracks of aforementioned weather condition, where the format mentioned is: Song Name##Album Name##Artists Name.
                             Also, recommend $numOfQuery related music tracks of the related emotion of this weather and time, where the format mentioned is: Song Name##Album Name##Artists Name.
                             Notice: List only one related music track in each row using format: Song Name##Album Name##Artists Name 
                """

            val responseRelated = geminiApi.askGeminiHome(prompt)

            var outputContent = responseRelated.text?.trimIndent()
            if (outputContent == null) {
                emit(UiState.Error("Failed to get a valid response from Gemini."))
                return@flow // End the Flow
            }
            // Though Schema mode usually does not return Markdown (```json), it is a good practice to clean the logic
            if (outputContent.startsWith("```")) {
                outputContent = outputContent.replace(Regex("^```json|^```|```$"), "").trim()
            }
            Log.d(musicTag, "trimmed response: $outputContent")

            val resultObj = gson.fromJson(outputContent, RecommendationResponse::class.java)
            Log.d(musicTag, "resultObj: $resultObj")
            val relatedTracksOfCondition = resultObj.weatherTracks
            val relatedTracksOfEmotion = resultObj.emotionTracks

            val conditionTempList = mutableListOf<SpotifyTrack>()
            val conditionNotFoundList = mutableListOf<String>()
            val emotionTempList = mutableListOf<SpotifyTrack>()
            val emotionNotFoundList = mutableListOf<String>()


            val batchSize = 20
            coroutineScope {
                // Deal with the batch query of relatedTracks
                relatedTracksOfCondition.chunked(batchSize).forEach { trackBatch ->
                    val deferredTrackResults = trackBatch.map { trackInfo ->
                        async { // Inherit Dispatchers.IO
                            //val parts = trackInfo.split("##")
                            if (trackInfo.trackName.isNotEmpty() && trackInfo.albumName.isNotEmpty() && trackInfo.artists.isNotEmpty()) {
                                val artists = trackInfo.artists.joinToString(",")
                                searchForSpecificTrackUseCase(trackInfo.trackName, trackInfo.albumName, artists)
                            } else {
                                Log.e(musicTag, "Unexpected track format: $trackInfo")
                                Pair(null, trackInfo.toString()) // Format error also consider as not found
                            }
                        }
                    }
                    // Wait for all tracks in the chunk to be fetched
                    val trackResults = deferredTrackResults.awaitAll()
                    Log.d(musicTag, "Chunk search finished.")
                    trackResults.forEach { result ->
                        val (track, notFoundId) = result
                        if (track != null) {
                            conditionTempList.add(track)
                        } else if (notFoundId != null) {
                            conditionNotFoundList.add(notFoundId)
                        }
                    }
                }

                // Deal with the batch query of relatedTracksOfEmotion
                relatedTracksOfEmotion.chunked(batchSize).forEach { trackBatch ->
                    val deferredTrackResults = trackBatch.map { trackInfo ->
                        async { // Inherit Dispatchers.IO
                            //val parts = trackInfo.split("##")
                            if (trackInfo.trackName.isNotEmpty() && trackInfo.albumName.isNotEmpty() && trackInfo.artists.isNotEmpty()) {
                                val artists = trackInfo.artists.joinToString(",")
                                searchForSpecificTrackUseCase(trackInfo.trackName, trackInfo.albumName, artists)
                            } else {
                                Log.e(musicTag, "Unexpected track format: $trackInfo")
                                Pair(null, trackInfo.toString()) // Format error also consider as not found
                            }
                        }
                    }
                    // Wait for all tracks in the chunk to be fetched
                    val trackResults = deferredTrackResults.awaitAll()
                    Log.d(musicTag, "Chunk search finished.")
                    trackResults.forEach { result ->
                        val (track, notFoundId) = result
                        if (track != null) {
                            emotionTempList.add(track)
                        } else if (notFoundId != null) {
                            emotionNotFoundList.add(notFoundId)
                        }
                    }
                }
            }

            Log.d(musicTag, "conditionNotFoundList: $conditionNotFoundList")
            Log.d(musicTag, "emotionNotFoundList: $emotionNotFoundList")
            Log.d(musicTag, "conditionTempList: ${conditionTempList.joinToString { it.name }}")
            Log.d(musicTag, "emotionTempList: ${emotionTempList.joinToString { it.name }}")

            val data = TwoTracksList(
                conditionTempList.toList(),
                emotionTempList.toList(),
            )
            emit(UiState.Success(data))

        } catch (e: Exception) {
            // 4. Emit Error state if any error occurs
            Log.e("FindWeatherMusicUseCase", "Error: $e")
            e.printStackTrace()
            emit(UiState.Error(e.localizedMessage ?: "An unknown error occurred", e))
        }
    }.flowOn(Dispatchers.IO) // 5. Ensure all time-consuming operations are executed on the IO thread
}