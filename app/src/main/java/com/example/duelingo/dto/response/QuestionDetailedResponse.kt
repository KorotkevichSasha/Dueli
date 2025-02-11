package com.example.duelingo.dto.response
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuestionDetailedResponse(
    val difficulty: String,
    val type: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswers: List<String>,
    val audioUrl: String
): Parcelable