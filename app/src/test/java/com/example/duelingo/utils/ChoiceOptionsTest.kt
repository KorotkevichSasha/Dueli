package com.example.duelingo.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChoiceOptionsTest {
    @Test
    fun `adds a real distractor instead of a meta answer`() {
        val result = ChoiceOptions.withFourthDistractor(
            listOf("to", "for", "with"),
            listOf("to")
        )

        assertEquals(4, result.size)
        assertEquals("at", result.last())
        assertFalse(result.any { it.contains("none", ignoreCase = true) })
    }

    @Test
    fun `never duplicates an option or a correct answer`() {
        val result = ChoiceOptions.withFourthDistractor(
            listOf("at", "to", "with"),
            listOf("by")
        )

        assertEquals(4, result.size)
        assertEquals(4, result.map(String::lowercase).distinct().size)
        assertTrue(result.last() !in listOf("at", "to", "with", "by"))
    }

    @Test
    fun `keeps already balanced questions unchanged`() {
        val options = listOf("studies", "study", "studied", "is")
        assertEquals(options, ChoiceOptions.withFourthDistractor(options, listOf("studies")))
    }
}
