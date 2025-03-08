package com.example.duelingo.dto.response

data class AudioQuestionResponse(
    val id: String,
    val type: String,
    val questionText: String,
    val audioUrl: String?
)