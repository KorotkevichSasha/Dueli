package com.example.duelingo.activity.auth

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.activity.MenuActivity
import com.example.duelingo.databinding.ActivityLoginBinding
import com.example.duelingo.dto.request.SignInRequest
import com.example.duelingo.dto.response.JwtAuthenticationResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.AuthSessionManager
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_EXPIRED = "session_expired"
    }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.getBooleanExtra(EXTRA_SESSION_EXPIRED, false)) {
            showToast(getString(com.example.duelingo.R.string.session_expired))
        }

        binding.loginBtn.setOnClickListener {
            val username = binding.emailEt.text.toString()
            val password = binding.passwordEt.text.toString()

            when {
                username.isEmpty() || password.isEmpty() -> {
                    showToast(getString(com.example.duelingo.R.string.error_check_fields))
                }

                password.length < 8 -> {
                    showToast(getString(com.example.duelingo.R.string.error_password_length))
                }

                else -> {
                    val signInRequest = SignInRequest(username, password)
                    loginUser(signInRequest)
                }
            }
        }

        binding.noAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        binding.passwordEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.loginBtn.performClick()
                true
            } else false
        }
    }

    private fun loginUser(signInRequest: SignInRequest) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.authService.signIn(signInRequest)
                showToast(getString(com.example.duelingo.R.string.login_successful))

                saveTokens(response)

                startActivity(Intent(this@LoginActivity, MenuActivity::class.java))
                finish()

            } catch (e: HttpException) {
                handleServerError(e)

            } catch (e: Exception) {
                showToast(UserMessage.from(this@LoginActivity, e))
            }
        }
    }
    private fun saveTokens(response: JwtAuthenticationResponse) {
        val tokenManager = TokenManager(this)
        tokenManager.saveTokens(response.accessToken, response.refreshToken)
        AuthSessionManager.onAuthenticated()
    }


    private fun handleServerError(e: HttpException) {
        showToast(UserMessage.from(this, e))
    }


    private fun showToast(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }
}
