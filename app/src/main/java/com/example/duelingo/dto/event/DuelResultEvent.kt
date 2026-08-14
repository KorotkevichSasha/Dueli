package com.example.duelingo.dto.event

data class DuelResultEvent(
    val player1Score: Int,
    val player2Score: Int,
    val winner: String,
    val forfeitedBy: String? = null
)
