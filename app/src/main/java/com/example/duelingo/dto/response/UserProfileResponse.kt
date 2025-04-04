package com.example.duelingo.dto.response

data class UserProfileResponse(
    val id: String,
    val username: String,
    val email: String,
    val points: Int,
    val avatarUrl: String,
    val lastLogin: String
)