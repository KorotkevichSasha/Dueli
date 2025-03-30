package com.example.duelingo.dto.response

import java.util.UUID
import com.google.gson.annotations.SerializedName
import java.util.Date

data class WordProgressResponse(
    @SerializedName("userId") val userId: UUID,
    @SerializedName("wordId") val wordId: UUID,
    @SerializedName("term") val term: String,
    @SerializedName("translation") val translation: String,
    @SerializedName("repetitions") val repetitions: Int,
    @SerializedName("easinessFactor") val easinessFactor: Double,

    @SerializedName("nextReviewDate") val nextReviewDate: String
) {
    fun getNextReviewDateAsDate(): Date? {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd").parse(nextReviewDate)
        } catch (e: Exception) {
            null
        }
    }
}