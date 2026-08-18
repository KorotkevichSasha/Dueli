package com.example.duelingo.dto.request

data class PasswordResetRequest(val email: String)

data class PasswordResetConfirmRequest(
    val email: String,
    val code: String,
    val newPassword: String
)
