package com.example.geminispotifyapp.utils

import org.apache.commons.text.similarity.LevenshteinDistance

object StringSimilarityCalculator {
    fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.lowercase()
        val s2 = str2.lowercase()
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0
        }
        // Use Levenshtein Distance to calculate the edit distance between the two strings.
        val editDistance = LevenshteinDistance.getDefaultInstance().apply(s1, s2)

        // Calculate the similarity as 1 - (edit distance / max length of the two strings).
        val maxLength = maxOf(s1.length, s2.length)
        val similarity = if (maxLength > 0) {
            1.0 - (editDistance.toDouble() / maxLength)
        } else {
            1.0 // If both strings are empty, similarity is 1.0.
        }

        return similarity
    }
}