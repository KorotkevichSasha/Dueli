package com.example.duelingo.dto.response

data class EconomyResponse(
    val gold: Int,
    val rushCharges: Int,
    val maxRushCharges: Int,
    val nextRushChargeAt: String?,
    val minutesPerCharge: Int,
    val ratingPoints: Int,
    val league: LeagueResponse
)
