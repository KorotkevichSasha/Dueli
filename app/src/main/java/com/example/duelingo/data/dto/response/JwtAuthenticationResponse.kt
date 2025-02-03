package com.example.duelingo.data.dto.response

data class JwtAuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
)