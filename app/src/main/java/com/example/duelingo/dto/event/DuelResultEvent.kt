package com.example.duelingo.dto.event

data class DuelResultEvent(
    val player1Points: Int,
    val player2Points: Int,
    val winner: String
) 