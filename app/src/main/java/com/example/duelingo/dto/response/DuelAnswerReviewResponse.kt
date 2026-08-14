package com.example.duelingo.dto.response

data class DuelAnswerReviewResponse(
    val questionNumber: Int,
    val questionText: String,
    val type: String,
    val submittedAnswer: String,
    val correctAnswer: String,
    val correct: Boolean
)
