package com.example.duelingo.dto.response

data class JwtAuthenticationResponse(
    val accessToken: String,
    val refreshToken: String
)