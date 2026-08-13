package com.example.duelingo

import com.example.duelingo.utils.OfflineDuelFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDuelFactoryTest {
    @Test
    fun everyLevelContainsTenValidUniqueQuestions() {
        listOf("EASY", "MEDIUM", "HARD").forEach { level ->
            val event = OfflineDuelFactory.create(level)
            val questions = event.duel.questions
            assertEquals(10, questions.size)
            assertEquals(10, questions.map { it.id }.distinct().size)
            assertTrue(questions.all { it.correctAnswers.isNotEmpty() })
            assertTrue(questions.filter { it.type == "FILL_IN_CHOICE" }
                .all { it.options.containsAll(it.correctAnswers) })
            assertTrue(questions.filter { it.type == "SENTENCE_CONSTRUCTION" }
                .all { it.questionText.contains(Regex("[А-Яа-яЁё]")) })
        }
    }
}
