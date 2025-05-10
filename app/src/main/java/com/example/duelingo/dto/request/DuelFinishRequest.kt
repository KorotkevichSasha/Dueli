package com.example.duelingo.dto.request

data class DuelFinishRequest(
    val duelId: String,
    val correctAnswers: Int,
    val timeSpent: Long
)