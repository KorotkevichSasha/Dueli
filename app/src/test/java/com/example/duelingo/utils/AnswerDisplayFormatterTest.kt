package com.example.duelingo.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AnswerDisplayFormatterTest {
    @Test
    fun `shows one canonical answer instead of slash separated alternatives`() {
        assertEquals(
            "practices",
            AnswerDisplayFormatter.canonical(listOf("practices", "plays"))
        )
    }

    @Test
    fun `shows accepted submitted alternative when it is correct`() {
        assertEquals(
            "safely stopped before the crossing",
            AnswerDisplayFormatter.canonical(
                listOf("stopped safely before the crossing", "safely stopped before the crossing"),
                "Safely stopped before the crossing."
            )
        )
    }

    @Test
    fun `cleans slash separated history produced by an older server`() {
        assertEquals(
            "stopped safely",
            AnswerDisplayFormatter.canonicalLegacy("stopped safely / safely stopped")
        )
    }
}
