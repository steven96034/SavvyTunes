package com.example.geminispotifyapp.data.remote.api

import com.example.geminispotifyapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiApi @Inject constructor(){
    // For Gemini Model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.apiKey
    )

    suspend fun askGemini(prompt: String): GenerateContentResponse =
        try {
            generativeModel.generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }
}