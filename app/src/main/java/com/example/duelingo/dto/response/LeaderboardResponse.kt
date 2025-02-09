package com.example.duelingo.dto.response

data class LeaderboardResponse(
    val top: PaginationResponse<UserInLeaderboardResponse>,
    val currentUser: UserInLeaderboardResponse
)