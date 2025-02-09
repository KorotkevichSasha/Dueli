package com.example.duelingo.network

import com.example.duelingo.dto.response.UserProfileResponse
import retrofit2.http.GET
import retrofit2.http.Header
interface ProfileService {

    @GET("/users/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse
}