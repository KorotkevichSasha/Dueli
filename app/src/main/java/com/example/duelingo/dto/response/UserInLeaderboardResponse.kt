package com.example.duelingo.dto.response

data class UserInLeaderboardResponse(
    val id: String,
    val username: String,
    val points: Int,
    val avatarUrl: String,
    val rank: Long
)
