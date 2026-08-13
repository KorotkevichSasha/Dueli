package com.example.duelingo.network

import com.example.duelingo.dto.request.SignInRequest
import com.example.duelingo.dto.request.SignUpRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import com.example.duelingo.dto.response.RegistrationResponse
import com.example.duelingo.dto.request.VerifyEmailRequest
import com.example.duelingo.dto.request.ResendVerificationRequest
import retrofit2.http.Body
import retrofit2.http.POST

data class RefreshTokenRequest(val refreshToken: String)

interface AuthService {
    @POST("auth/sign-in")
    suspend fun signIn(@Body request: SignInRequest): JwtAuthenticationResponse

    @POST("auth/sign-up")
    suspend fun signUp(@Body request: SignUpRequest): RegistrationResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): JwtAuthenticationResponse

    @POST("auth/resend-verification")
    suspend fun resendVerification(@Body request: ResendVerificationRequest): RegistrationResponse

    @POST("auth/refresh-token")
    suspend fun refresh(@Body request: RefreshTokenRequest): JwtAuthenticationResponse
}
