package com.example.duelingo.network

import com.example.duelingo.dto.request.SignInRequest
import com.example.duelingo.dto.request.SignUpRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): JwtAuthenticationResponse

    @POST("auth/sign-up")
    suspend fun signUp(@Body request: SignUpRequest): JwtAuthenticationResponse

    @POST("auth/refresh-token")
    suspend fun refresh(@Body refreshToken: String): JwtAuthenticationResponse
}