package com.example.duelingo.dto.response

import java.util.UUID

data class FriendResponse(
    val id: UUID,
    val username: String,
    val points: Int,
    val avatarUrl: String?
)