package com.example.duelingo.utils

import com.example.duelingo.dto.response.QuestionDetailedResponse
import java.util.Locale

object AnswerEvaluator {
    fun matches(question: QuestionDetailedResponse, answer: String): Boolean {
        if (question.correctAnswers.isEmpty()) return false
        return question.correctAnswers.any { expected ->
            val normalizedExpected = normalize(expected)
            val normalizedActual = normalize(answer)
            normalizedExpected == normalizedActual ||
                (question.type == "SENTENCE_CONSTRUCTION" &&
                    equivalentWithMovableAdverbs(normalizedExpected, normalizedActual))
        }
    }

    fun normalize(input: String): String = input
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}\\p{N}']+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun equivalentWithMovableAdverbs(expected: String, actual: String): Boolean {
        val expectedWords = expected.split(' ').filter(String::isNotBlank)
        val actualWords = actual.split(' ').filter(String::isNotBlank)
        if (expectedWords.size != actualWords.size) return false
        val isMovable: (String) -> Boolean = { word ->
            word.endsWith("ly") && word.length > 3 && word !in setOf("only", "early", "likely")
        }
        val expectedAdverbs = expectedWords.filter(isMovable).sorted()
        if (expectedAdverbs.isEmpty()) return false
        return expectedAdverbs == actualWords.filter(isMovable).sorted() &&
            expectedWords.filterNot(isMovable) == actualWords.filterNot(isMovable)
    }
}
