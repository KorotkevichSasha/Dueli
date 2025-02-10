package com.example.duelingo.activity.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.activity.MenuActivity
import com.example.duelingo.databinding.ActivityLoginBinding
import com.example.duelingo.dto.request.SignInRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginBtn.setOnClickListener {
            val username = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            when {
                username.isEmpty() || password.isEmpty() -> {
                    showToast("Fields cannot be empty")
                }

                password.length < 8 -> {
                    showToast("Password must be at least 8 characters")
                }

                else -> {
                    val signInRequest = SignInRequest(username, password)
                    loginUser(signInRequest)
                }
            }
        }

        binding.signupRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser(signInRequest: SignInRequest) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.signIn(signInRequest)
                showToast("Login Successful")

                saveTokens(response)

                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                finish()

            } catch (e: HttpException) {
                handleServerError(e)

            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            }
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


    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
