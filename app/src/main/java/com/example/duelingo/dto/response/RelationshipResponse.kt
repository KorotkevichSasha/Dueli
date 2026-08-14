package com.example.duelingo.dto.response

import java.util.UUID

data class RelationshipResponse(
    val id: UUID,
    val fromUserId: UUID,
    val fromUsername: String,
    val fromAvatarUrl: String?,
    val toUserId: UUID,
    val toUsername: String,
    val toAvatarUrl: String?,
    val status: RelationshipStatus
)
