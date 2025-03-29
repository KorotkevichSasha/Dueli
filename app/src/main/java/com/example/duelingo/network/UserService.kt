package com.example.duelingo.network

import com.example.duelingo.dto.response.FriendResponse
import com.example.duelingo.dto.response.PaginationResponse
import com.example.duelingo.dto.response.UserProfileResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface UserService {

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

    @GET("/users/avatar/{userId}")
    suspend fun getAvatar(
        @Header("Authorization") token: String,
        @Header("If-None-Match") eTag: String? = null,
        @Path("userId") userId: String
    ): retrofit2.Response<ResponseBody>

    @GET("/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): PaginationResponse<FriendResponse>

    @GET("/users/friends")
    suspend fun getCurrentUserFriends(
        @Header("Authorization") token: String,
    ): List<FriendResponse>

    @GET("/users/{userId}/friends")
    suspend fun getUserFriends(
        @Header("Authorization") token: String,
        @Path("userId") userId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): List<FriendResponse>
}