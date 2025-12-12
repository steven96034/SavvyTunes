package com.example.geminispotifyapp.data.remote.api

import com.example.geminispotifyapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
//import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.firebase.ai.type.GenerateContentResponse
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApi @Inject constructor(){
//    // For Gemini Model
//    private val generativeModel = GenerativeModel(
//        modelName = "gemini-2.5-flash",
//        apiKey = BuildConfig.apiKey
//    )
    private val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI())
        .generativeModel("gemini-2.5-flash")

    suspend fun askGemini(prompt: String): GenerateContentResponse =
        try {
            generativeModel.generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }
}