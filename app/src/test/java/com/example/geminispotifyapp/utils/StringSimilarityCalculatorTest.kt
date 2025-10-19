package com.example.geminispotifyapp.utils

import com.example.geminispotifyapp.core.utils.StringSimilarityCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class StringSimilarityCalculatorTest {
    private val delta = 0.0001

    @Test
    fun `calculateSimilarity with identical strings should return 1`() {
        // Arrange
        val str1 = "hello world"
        val str2 = "hello world"

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(1.0, similarity, delta)
    }

    @Test
    fun `calculateSimilarity with strings differing only by case should return 1`() {
        // Arrange
        val str1 = "Kotlin"
        val str2 = "kotlin"

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(1.0, similarity, delta)
    }

    @Test
    fun `calculateSimilarity with completely different strings of same length should return 0`() {
        // Arrange
        val str1 = "abc"
        val str2 = "xyz"
        // Levenshtein distance is 3. Max length is 3. (From abc: a -> x, b -> y, c -> z, total 3 steps of distance.)
        // Similarity = 1.0 - (3.0 / 3.0) = 0.0

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(0.0, similarity, delta)
    }

    @Test
    fun `calculateSimilarity with partial match should return correct similarity score`() {
        // Arrange
        val str1 = "kitten"
        val str2 = "sitting"
        // Levenshtein distance is 3. Max length is 7. (From kitten: k -> s, e -> i, insert g, total 3 steps of distance.)
        // Similarity = 1.0 - (3.0 / 7.0) = 0.5714...

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(1.0 - (3.0 / 7.0), similarity, delta)
    }

    @Test
    fun `calculateSimilarity with strings of different length should calculate correctly`() {
        // Arrange
        val str1 = "book"
        val str2 = "books"
        // Levenshtein distance is 1. Max length is 5. (From book: insert s, total 1 step of distance.)
        // Similarity = 1.0 - (1.0 / 5.0) = 0.8

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(0.8, similarity, delta)
    }

    @Test
    fun `calculateSimilarity when first string is empty should return 0`() {
        // Arrange
        val str1 = ""
        val str2 = "test"

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(0.0, similarity, delta)
    }

    @Test
    fun `calculateSimilarity when second string is empty should return 0`() {
        // Arrange
        val str1 = "test"
        val str2 = ""

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(0.0, similarity, delta)
    }

    @Test
    fun `calculateSimilarity when both strings are empty should return 0`() {
        // Arrange
        val str1 = ""
        val str2 = ""

        // Act
        val similarity = StringSimilarityCalculator.calculateSimilarity(str1, str2)

        // Assert
        assertEquals(0.0, similarity, delta)
    }
}