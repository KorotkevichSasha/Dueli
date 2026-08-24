package com.example.duelingo.dto.event

data class DuelResultEvent(
    val player1Score: Int,
    val player2Score: Int,
    val winner: String,
    val forfeitedBy: String? = null,
    val goldAwarded: Int = 0,
    val ratingDelta: Int = 0,
    val leagueId: String? = null,
    val leaguePromoted: Boolean = false,
    val previousLeagueId: String? = null,
    val leagueBonusGold: Int = 0
)
