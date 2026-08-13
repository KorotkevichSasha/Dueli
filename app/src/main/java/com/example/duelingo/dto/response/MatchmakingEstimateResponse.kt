package com.example.duelingo.dto.response

data class MatchmakingEstimateResponse(
    val difficulty: String,
    val averageWaitSeconds: Long,
    val playersWaiting: Long
)
