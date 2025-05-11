package com.example.duelingo.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.adapters.AchievementsAdapter
import com.example.duelingo.databinding.ActivityAchievementsBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class AchievementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAchievementsBinding
    private lateinit var adapter: AchievementsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAchievementsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadAchievements()
    }

    private fun setupRecyclerView() {
        adapter = AchievementsAdapter(emptyList())
        binding.achievementRecycler.layoutManager = LinearLayoutManager(this)
        binding.achievementRecycler.adapter = adapter
    }

    private fun loadAchievements() {
        val tokenManager = TokenManager(this)
        val token = tokenManager.getAccessToken()

        if (token != null) {
            val bearerToken = "Bearer $token"
            lifecycleScope.launch {
                try {
                    val achievements = ApiClient.achievementService.getUserAchievements(bearerToken)
                    adapter.updateData(achievements)
                } catch (e: Exception) {
                    showToast("Ошибка загрузки: ${e.localizedMessage}")
                }
            }
        } else {
            showToast("Ошибка авторизации")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}