package com.example.duelingo.network

import com.example.duelingo.dto.request.RelationshipRequest
import com.example.duelingo.dto.response.RelationshipResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
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

    @PATCH("/relationships/friend-requests/{requestId}")
    suspend fun updateRelationshipStatus(
        @Header("Authorization") token: String,
        @Path("requestId") requestId: UUID,
        @Query("action") action: String
    ): Response<RelationshipResponse>
}