package com.example.duelingo.network

import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.request.UserReportRequest
import com.example.duelingo.dto.response.RelationshipResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.UUID


interface RelationshipService {
    @POST("/relationships/friend-requests")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body request: RelationshipRequest
    ): Response<Unit>

    @GET("/relationships/friend-requests/incoming")
    suspend fun getIncomingRequests(
        @Header("Authorization") token: String
    ): Response<List<RelationshipResponse>>

    @GET("/relationships/friend-requests/outgoing")
    suspend fun getOutgoingRequests(@Header("Authorization") token: String): Response<List<RelationshipResponse>>

    @DELETE("/relationships/friend-requests/{requestId}")
    suspend fun cancelOutgoingRequest(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: UUID
    ): Response<Unit>

    @DELETE("/relationships/friends/{friendId}")
    suspend fun removeFriend(
        @Header("Authorization") token: String,
        @Path("friendId") friendId: UUID
    ): Response<Unit>

    @POST("/relationships/blocks")
    suspend fun blockUser(
        @Header("Authorization") token: String,
        @Body request: RelationshipRequest
    ): Response<RelationshipResponse>

    @POST("/relationships/reports")
    suspend fun reportUser(
        @Header("Authorization") token: String,
        @Body request: UserReportRequest
    ): Response<Unit>

    @PATCH("/relationships/friend-requests/{requestId}")
    suspend fun updateRelationshipStatus(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: UUID,
        @Query("action") action: String
    ): Response<RelationshipResponse>
}
