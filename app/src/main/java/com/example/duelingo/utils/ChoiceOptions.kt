package com.example.duelingo.utils

/** Keeps multiple-choice questions visually balanced without exposing a meta answer. */
object ChoiceOptions {
    private val fallbackDistractors = listOf("at", "by", "was", "never", "another", "having")

    fun withFourthDistractor(options: List<String>, correctAnswers: List<String>): List<String> {
        if (options.size != 3) return options
        val used = (options + correctAnswers).map { it.trim().lowercase() }.toSet()
        val distractor = fallbackDistractors.first { it.lowercase() !in used }
        return options + distractor
    }
}
