package com.example.geminispotifyapp.core.utils

import org.apache.commons.text.similarity.LevenshteinDistance

object StringSimilarityCalculator {
    fun calculateSimilarity(str1: String, str2: String): Double {
        val s1 = str1.lowercase()
        val s2 = str2.lowercase()

        // If either string is empty, they are considered completely dissimilar.
        if (s1.isEmpty() || s2.isEmpty()) {
            return 0.0
        }
        // Use Levenshtein Distance to calculate the edit distance between the two strings.
        val editDistance = LevenshteinDistance.getDefaultInstance().apply(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)

        // Calculate the similarity as 1 - (edit distance / max length of the two strings).
        return 1.0 - (editDistance.toDouble() / maxLength)
    }
}