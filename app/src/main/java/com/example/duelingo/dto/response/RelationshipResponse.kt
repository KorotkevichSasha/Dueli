package com.example.duelingo.dto.response

import java.util.UUID

data class RelationshipResponse(
    val id: UUID,
    val fromUserId: UUID,
    val fromUsername: String,
    val fromUserAvatarUrl: String?,
    val toUserId: UUID,
    val status: RelationshipStatus
)
