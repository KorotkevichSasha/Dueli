package com.example.duelingo.utils

/** Keeps accepted alternatives internal and presents one clear answer to the learner. */
object AnswerDisplayFormatter {
    private val legacySeparator = Regex("\\s+/\\s+")

    fun canonical(answers: List<String>, submittedAnswer: String = ""): String {
        val nonBlank = answers.map(String::trim).filter(String::isNotBlank)
        if (nonBlank.isEmpty()) return ""
        val submitted = AnswerEvaluator.normalize(submittedAnswer)
        return nonBlank.firstOrNull { AnswerEvaluator.normalize(it) == submitted }
            ?: nonBlank.first()
    }

    /** Handles duel history saved by older server versions as "answer / alternative". */
    fun canonicalLegacy(value: String): String = value
        .split(legacySeparator, limit = 2)
        .firstOrNull()
        .orEmpty()
        .trim()
}
