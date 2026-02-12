package com.example.geminispotifyapp.data.remote.api

//import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApi @Inject constructor(){
//    // For Gemini Model
//    private val generativeModel = GenerativeModel(
//        modelName = "gemini-2.5-flash",
//        apiKey = BuildConfig.apiKey
//    )
    private val generativeModelBase = Firebase.ai(backend = GenerativeBackend.googleAI())


    // Define the structure of a single track (track(String), album(String), artists(List<String>))
    private val trackItemSchema = Schema.obj(
        mapOf(
            "trackName" to Schema.string(),
            "albumName" to Schema.string(),
            "artists" to Schema.array(Schema.string())
        )
    )

    // Define outer structure from schema
    private val homeJsonSchema = Schema.obj(
        mapOf(
            "weatherTracks" to Schema.array(trackItemSchema),
            "emotionTracks" to Schema.array(trackItemSchema)
        )
    )
    val homeConfig = generationConfig {
        temperature = 0.4f
        responseMimeType = "application/json"
        responseSchema = homeJsonSchema
    }
    suspend fun askGeminiHome(prompt: String): GenerateContentResponse =
        try {
            generativeModelBase
                .generativeModel("gemini-2.5-flash", homeConfig)
                .generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }



    val findMusicJsonSchema = Schema.obj(
        mapOf(
            "similarTracks" to Schema.array(trackItemSchema),
            "similarArtists" to Schema.array(trackItemSchema)
        )
    )
    val findMusicConfig = generationConfig {
        temperature = 0.4f
        responseMimeType = "application/json"
        responseSchema = findMusicJsonSchema
    }
    suspend fun askGeminiFindMusic(prompt: String): GenerateContentResponse =
        try {
            generativeModelBase
                .generativeModel("gemini-2.5-flash", findMusicConfig)
                .generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }
    
    private val userPreferencesSchema = Schema.obj(
        mapOf(
            "genre" to Schema.string(),
            "year" to Schema.string(),
            "language" to Schema.string()
        )
    )

    val userPreferencesConfig = generationConfig {
        temperature = 0.4f
        responseMimeType = "application/json"
        responseSchema = userPreferencesSchema
    }

    suspend fun askGeminiForUserPreferences(prompt: String): GenerateContentResponse =
        try {
            generativeModelBase
                .generativeModel("gemini-2.5-flash", userPreferencesConfig)
                .generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }
}