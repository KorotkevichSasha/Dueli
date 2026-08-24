package com.example.duelingo.dto.response

data class AchievementClaimResponse(
    val claimedGold: Int,
    val totalGold: Int,
    val achievement: UserAchievementResponse
)
