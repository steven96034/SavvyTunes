package com.example.geminispotifyapp.utils

import com.example.geminispotifyapp.core.utils.safeLet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class UIUtilsTest {
    @Test
    fun `safeLet WHEN all arguments are non-null THEN executes block and returns its result`() {
        // Arrange
        val p1 = "Hello"
        val p2 = 123
        val p3 = true
        var blockWasExecuted = false // Use a flag to confirm that the block has been executed


        // Act
        val result = safeLet(p1, p2, p3) { s, i, b ->
            blockWasExecuted = true
            // Simulate the operation inside the block and return a result
            "Result: $s, $i, $b"
        }

        // Assert
        assertTrue(blockWasExecuted, "Block should have been executed")
        assertEquals("Result: Hello, 123, true", result)
    }

    @Test
    fun `safeLet WHEN first argument is null THEN does not execute block and returns null`() {
        // Arrange
        val p1: String? = null
        val p2 = 123
        val p3 = true
        var blockWasExecuted = false

        // Act
        val result = safeLet(p1, p2, p3) { _, _, _ ->
            blockWasExecuted = true // This line should not be executed
            "Should not reach here"
        }

        // Assert
        assertFalse(blockWasExecuted, "Block should not have been executed")
        assertNull(result)
    }

    @Test
    fun `safeLet WHEN second argument is null THEN does not execute block and returns null`() {
        // Arrange
        val p1 = "Hello"
        val p2: Int? = null
        val p3 = true
        var blockWasExecuted = false

        // Act
        val result = safeLet(p1, p2, p3) { _, _, _ ->
            blockWasExecuted = true
            "Should not reach here"
        }

        // Assert
        assertFalse(blockWasExecuted, "Block should not have been executed")
        assertNull(result)
    }

    @Test
    fun `safeLet WHEN third argument is null THEN does not execute block and returns null`() {
        // Arrange
        val p1 = "Hello"
        val p2 = 123
        val p3: Boolean? = null
        var blockWasExecuted = false

        // Act
        val result = safeLet(p1, p2, p3) { _, _, _ ->
            blockWasExecuted = true
            "Should not reach here"
        }

        // Assert
        assertFalse(blockWasExecuted, "Block should not have been executed")
        assertNull(result)
    }

    @Test
    fun `safeLet WHEN all arguments are null THEN does not execute block and returns null`() {
        // Arrange
        val p1: String? = null
        val p2: Int? = null
        val p3: Boolean? = null
        var blockWasExecuted = false

        // Act
        val result = safeLet(p1, p2, p3) { _, _, _ ->
            blockWasExecuted = true
            "Should not reach here"
        }

        // Assert
        assertFalse(blockWasExecuted, "Block should not have been executed")
        assertNull(result)
    }
}