package com.example.duelingo.dto.response

data class AudioAnswerResponse(
    val isCorrect: Boolean,
    val recognizedText: String,
    val feedback: String
)