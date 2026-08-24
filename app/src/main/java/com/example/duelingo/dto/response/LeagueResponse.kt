package com.example.duelingo.dto.response

data class LeagueResponse(
    val id: String,
    val name: String,
    val minimumPoints: Int,
    val nextLeaguePoints: Int?,
    val progressPercent: Int,
    val pointsToNextLeague: Int?
)
