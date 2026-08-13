package com.example.duelingo.dto.response

data class RegistrationResponse(
    val email: String,
    val verificationRequired: Boolean,
    val emailSent: Boolean
)
