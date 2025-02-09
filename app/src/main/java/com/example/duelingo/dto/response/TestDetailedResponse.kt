package com.example.duelingo.dto.response

data class TestDetailedResponse(
    val id: String,
    val topic: String,
    val difficulty: String,
    val questions: List<QuestionDetailedResponse>
)