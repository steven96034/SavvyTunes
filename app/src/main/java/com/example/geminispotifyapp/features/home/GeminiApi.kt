package com.example.geminispotifyapp.features.home

import com.example.geminispotifyapp.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.CodeExecutionResultPart
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.ExecutableCodePart
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.flow.Flow

class GeminiApi {
    // For Gemini Model
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-pro",
        apiKey = BuildConfig.apiKey
    )

    suspend fun askGemini(prompt: String): GenerateContentResponse =
        try {
            generativeModel.generateContent(prompt)
        } catch (e: Exception) {
            throw e
        }


    // Use flow in ViewModel
    // flow.collect { value ->         println("Received $value")     }
    fun askGeminiStreaming(prompt: Content): Flow<GenerateContentResponse> =
        try {
            generativeModel.generateContentStream(prompt)
        }
        catch (e: Exception) {
            throw e
        }

    fun getIndexedCandidateResponse(response: GenerateContentResponse, index: Int): String {
        if (index >= response.candidates.size) {
            throw IndexOutOfBoundsException("Index $index is out of bounds for ${response.candidates.size} candidates")
        }
        return response.candidates[index]
                .content
                .parts
                .filter { it is TextPart || it is ExecutableCodePart || it is CodeExecutionResultPart }
                .joinToString(" ") {
                    when (it) {
                        is TextPart -> it.text
                        is ExecutableCodePart -> "\n```${it.language.lowercase()}\n${it.code}\n```"
                        is CodeExecutionResultPart -> "\n```\n${it.output}\n```"
                        else -> throw RuntimeException("unreachable")
                    }
                }
    }

    /*
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                }
            )
     */
}