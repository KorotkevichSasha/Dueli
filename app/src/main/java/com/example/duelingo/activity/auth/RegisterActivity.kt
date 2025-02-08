package com.example.duelingo.activity.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.activity.MenuActivity
import com.example.duelingo.databinding.ActivityRegisterBinding
import com.example.duelingo.dto.request.SignUpRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backBtn.setOnClickListener {
            onBackPressed()
        }

        binding.loginBtn.setOnClickListener {
            val email = binding.email.text.toString()
            val username = binding.username.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()

            when {
                email.isEmpty() || password.isEmpty() || username.isEmpty() || confirmPassword.isEmpty() -> {
                    showToast("Fields cannot be empty")
                }

                password != confirmPassword -> {
                    showToast("Passwords must match")
                }

                password.length < 8 -> {
                    showToast("Password must be at least 8 characters")
                }

                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showToast("Please enter a valid email address")
                }

                else -> {
                    val signUpRequest = SignUpRequest(username, email, password)
                    registerUser(signUpRequest)
                }
            }
        }
    }

    private fun registerUser(signUpRequest: SignUpRequest) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.signUp(signUpRequest)
                if (response.accessToken.isNotEmpty()) {
                    saveTokens(response)
                    startActivity(Intent(this@RegisterActivity, MenuActivity::class.java))
                } else {
                    showToast("Registration failed")
                }
            } catch (e: HttpException) {
                handleServerError(e)
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
        }
    }

    private fun handleServerError(e: HttpException) {
        val errorBody = e.response()?.errorBody()?.string()

        if (errorBody != null) {
            try {
                val json = JSONObject(errorBody)
                val errorMessage = StringBuilder()
                json.keys().forEach { key ->
                    errorMessage.append(json.getString(key)).append("\n")
                }
                showToast(errorMessage.toString().trim())
            } catch (ex: Exception) {
                showToast("Server error: ${e.code()}")
            }
        } else {
            showToast("Server error: ${e.code()}")
        }
    }

    private fun saveTokens(response: JwtAuthenticationResponse) {
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("access_token", response.accessToken)
            putString("refresh_token", response.refreshToken)
            apply()
        }
        val tokenManager = TokenManager(this)
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
    }

    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
