package com.example.duelingo

import com.example.duelingo.dto.response.QuestionDetailedResponse
import com.example.duelingo.utils.AnswerEvaluator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerEvaluatorTest {

    @Test
    fun `ignores case punctuation and repeated whitespace`() {
        val question = question("I have already finished the task.")

        assertTrue(AnswerEvaluator.matches(question, "  i HAVE already finished the task!  "))
    }

    @Test
    fun `accepts explicitly supplied alternative sentence order`() {
        val question = question(
            "The driver stopped safely before the crossing.",
            "The driver safely stopped before the crossing."
        )

        assertTrue(AnswerEvaluator.matches(question, "The driver safely stopped before the crossing"))
    }

    @Test
    fun `allows a movable manner adverb but preserves the rest of the sentence`() {
        val question = question("The driver stopped safely before the crossing.")

        assertTrue(AnswerEvaluator.matches(question, "The driver safely stopped before the crossing."))
        assertFalse(AnswerEvaluator.matches(question, "The crossing stopped safely before the driver."))
    }

    private fun question(vararg answers: String) = QuestionDetailedResponse(
        id = "question-1",
        difficulty = "MEDIUM",
        type = "SENTENCE_CONSTRUCTION",
        questionText = "Составьте предложение",
        options = emptyList(),
        correctAnswers = answers.toList()
    )
}
