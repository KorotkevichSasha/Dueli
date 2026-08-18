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
import com.example.duelingo.storage.OnboardingPreferences
import com.example.duelingo.utils.ProfileCache
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
        if (accessToken.isNullOrBlank()) {
            openLogin()
            return
        }

        // Render the authenticated shell immediately after the first successful load.
        // Destination screens refresh their own data and the API interceptor still
        // handles an expired access token, so a network round-trip is not required
        // on every cold start.
        if (ProfileCache.read(this, accessToken) != null) {
            openAuthenticatedDestination()
            return
        }

        setChecking(true)
        lifecycleScope.launch {
            try {
                val profile = ApiClient.userService.getProfile("Bearer $accessToken")
                ProfileCache.store(this@MainActivity, accessToken, profile)
                openAuthenticatedDestination()
            } catch (error: HttpException) {
                if (error.code() == 401 || error.code() == 403) {
                    tokenManager.clearTokens()
                    openLogin()
                } else {
                    openCachedSessionOrShowError(accessToken)
                }
            } catch (_: IOException) {
                openCachedSessionOrShowError(accessToken)
            } catch (_: Exception) {
                openCachedSessionOrShowError(accessToken)
            }
        }
    }

    private fun openCachedSessionOrShowError(accessToken: String) {
        if (ProfileCache.read(this, accessToken) != null) {
            openAuthenticatedDestination()
        } else {
            showConnectionError()
        }
    }

    private fun openAuthenticatedDestination() {
        val destination = if (OnboardingPreferences(this).isComplete) {
            MenuActivity::class.java
        } else {
            OnboardingActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
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
