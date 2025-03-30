package com.example.duelingo.dto.response

data class DuelResponse(
    val id: String,
    val player1: UserInDuelResponse,
    val player2: UserInDuelResponse,
    val questions: List<QuestionDetailedResponse>
)