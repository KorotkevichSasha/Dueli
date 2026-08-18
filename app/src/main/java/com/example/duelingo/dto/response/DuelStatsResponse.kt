package com.example.duelingo.dto.response

data class DuelStatsResponse(
    val total: Long,
    val wins: Long,
    val losses: Long,
    val draws: Long,
    val winRate: Int
)
