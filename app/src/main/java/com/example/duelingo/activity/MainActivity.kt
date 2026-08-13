package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.databinding.ActivityMainBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager
    private var sessionCheckRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        tokenManager = TokenManager(this)
        binding.retryButton.setOnClickListener { verifySession() }

        verifySession()
    }

    private fun verifySession() {
        if (sessionCheckRunning) return

        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()
        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            openLogin()
            return
        }

        setChecking(true)
        lifecycleScope.launch {
            try {
                ApiClient.userService.getProfile("Bearer $accessToken")
                startActivity(Intent(this@MainActivity, MenuActivity::class.java))
                finish()
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 403) {
                    tokenManager.clearTokens()
                    openLogin()
                } else {
                    showConnectionError()
                }
            } catch (_: IOException) {
                showConnectionError()
            } catch (_: Exception) {
                showConnectionError()
            }
        }
    }

    private fun setChecking(checking: Boolean) {
        sessionCheckRunning = checking
        binding.startupProgress.visibility = if (checking) View.VISIBLE else View.GONE
        binding.retryButton.visibility = View.GONE
        binding.startupStatus.setText(R.string.startup_checking)
    }

    private fun showConnectionError() {
        sessionCheckRunning = false
        binding.startupProgress.visibility = View.GONE
        binding.retryButton.visibility = View.VISIBLE
        binding.startupStatus.setText(R.string.startup_server_unavailable)
    }

    private fun openLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
