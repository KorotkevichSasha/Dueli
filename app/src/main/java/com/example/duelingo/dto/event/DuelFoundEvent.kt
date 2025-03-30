package com.example.duelingo.dto.event

import com.example.duelingo.dto.response.DuelResponse

data class DuelFoundEvent(
    val duel: DuelResponse,
    val opponentId: String,
    val difficulty: String? = "Medium"
)