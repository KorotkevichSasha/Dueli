package com.example.duelingo.network

import com.example.duelingo.dto.response.UserProfileResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ProfileService {

    @GET("/users/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): UserProfileResponse

    @Multipart
    @POST("/users/profile/avatar")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): UserProfileResponse

    @GET("/avatar/{userId}")
    suspend fun getAvatar(
        @Header("Authorization") token: String,
        @Header("If-None-Match") eTag: String? = null,
        @Path("userId") userId: String
    ): retrofit2.Response<ResponseBody>

}