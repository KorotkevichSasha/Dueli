package com.example.duelingo.dto.response

data class TestSummaryResponse(
    val id: String,
    val topic: String,
    val difficulty: String,
    val isCompleted : Boolean
)