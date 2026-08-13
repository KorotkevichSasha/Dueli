package com.example.duelingo.dto.event

data class DuelChallengeEvent(
    val challengeId: String,
    val challengerId: String,
    val challengerUsername: String,
    val difficulty: String,
    val expiresAt: String
)
