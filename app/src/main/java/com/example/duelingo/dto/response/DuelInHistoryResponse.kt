package com.example.duelingo.dto.response

import java.util.UUID

data class DuelInHistoryResponse(
    val id: UUID,
    val player1: UserInDuelResponse,
    val player1Score: Int,
    val player1Time: Long,
    val player2: UserInDuelResponse,
    val player2Score: Int,
    val player2Time: Long
)