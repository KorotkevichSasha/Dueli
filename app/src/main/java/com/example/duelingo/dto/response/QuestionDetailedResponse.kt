package com.example.duelingo.dto.response

data class QuestionDetailedResponse(
    val difficulty: String,
    val type: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswers: List<String>,
    val audioUrl: String
)