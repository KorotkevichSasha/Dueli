package com.example.duelingo.service;

import com.example.duelingo.dto.request.SignInRequest;
import com.example.duelingo.dto.request.SignUpRequest;
import com.example.duelingo.dto.response.JwtAuthenticationResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    @POST("auth/sign-up")
    Call<JwtAuthenticationResponse> SignUp(@Body SignUpRequest signUpRequest);

    @POST("auth/sign-in")
    Call<JwtAuthenticationResponse> SignIn(@Body SignInRequest signInRequest);

}

