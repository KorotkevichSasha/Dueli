package com.example.duelingo.utils

data class QuestionPrompt(
    val text: String,
    val hint: String?
)

object QuestionPromptParser {
    private val hintPattern = Regex(
        pattern = """\s*\(подсказка:\s*(.+)\)\s*$""",
        option = RegexOption.IGNORE_CASE
    )

    fun parse(rawText: String): QuestionPrompt {
        val match = hintPattern.find(rawText) ?: return QuestionPrompt(rawText.trim(), null)
        val hint = match.groupValues[1].trim().takeIf(String::isNotEmpty)
        return QuestionPrompt(rawText.removeRange(match.range).trim(), hint)
    }
}
