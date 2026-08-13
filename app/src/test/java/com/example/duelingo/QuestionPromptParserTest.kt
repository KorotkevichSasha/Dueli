package com.example.duelingo

import com.example.duelingo.utils.QuestionPromptParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuestionPromptParserTest {

    @Test
    fun `separates a bundled hint from the question`() {
        val prompt = QuestionPromptParser.parse(
            "My sister ___ the piano every week. (подсказка: «заниматься на пианино»; Present Simple для she)"
        )

        assertEquals("My sister ___ the piano every week.", prompt.text)
        assertEquals("«заниматься на пианино»; Present Simple для she", prompt.hint)
    }

    @Test
    fun `keeps an ordinary question unchanged`() {
        val prompt = QuestionPromptParser.parse("Anna ___ English every week.")

        assertEquals("Anna ___ English every week.", prompt.text)
        assertNull(prompt.hint)
    }
}
