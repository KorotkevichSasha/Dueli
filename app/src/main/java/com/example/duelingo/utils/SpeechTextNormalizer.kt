package com.example.duelingo.utils

import java.util.Locale

/** Makes Android speech-recognition output comparable with written English. */
object SpeechTextNormalizer {
    private val numberWords = mapOf(
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9,
        "ten" to 10, "eleven" to 11, "twelve" to 12, "thirteen" to 13,
        "fourteen" to 14, "fifteen" to 15, "sixteen" to 16, "seventeen" to 17,
        "eighteen" to 18, "nineteen" to 19, "twenty" to 20, "thirty" to 30,
        "forty" to 40, "fifty" to 50, "sixty" to 60, "seventy" to 70,
        "eighty" to 80, "ninety" to 90
    )

    fun normalize(text: String): String {
        val tokens = text.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9']+"), " ")
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)

        val result = mutableListOf<String>()
        var index = 0
        while (index < tokens.size) {
            val value = numberWords[tokens[index]]
            if (value == null) {
                result += tokens[index++]
                continue
            }
            var total = value
            if (value in 20..90 && value % 10 == 0 && index + 1 < tokens.size) {
                numberWords[tokens[index + 1]]?.takeIf { it in 1..9 }?.let {
                    total += it
                    index++
                }
            }
            result += total.toString()
            index++
        }
        return result.joinToString(" ")
    }
}
