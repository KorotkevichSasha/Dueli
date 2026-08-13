package com.example.duelingo.dto.request

data class VerifyEmailRequest(val email: String, val code: String)

data class ResendVerificationRequest(val email: String)
