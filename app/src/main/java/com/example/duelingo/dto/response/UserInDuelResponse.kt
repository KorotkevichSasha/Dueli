package com.example.duelingo.dto.response

import java.util.UUID

data class UserInDuelResponse(
    val userId: UUID,
    val username: String,
    val points: Int,
    val avatarUrl: String?
)