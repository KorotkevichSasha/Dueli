package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.adapters.TopicsAdapter
import com.example.duelingo.databinding.ActivityTopicsBinding
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

// TopicsActivity.kt
class TopicsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTopicsBinding
    private lateinit var topicsAdapter: TopicsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopicsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadTopics()
    }

    private fun setupRecyclerView() {
        binding.rvTopics.layoutManager = LinearLayoutManager(this)
        topicsAdapter = TopicsAdapter(emptyList()) { topic ->
            val intent = Intent(this, TestActivity::class.java).apply {
                putExtra("topic", topic)
            }
            startActivity(intent)
        }
        binding.rvTopics.adapter = topicsAdapter
    }

    private fun loadTopics() {
        val tokenManager = TokenManager(this)
        val accessToken = tokenManager.getAccessToken()

        if (accessToken != null) {
            val tokenWithBearer = "Bearer $accessToken"

            lifecycleScope.launch {
                try {
                    val topics = ApiClient.testService.getUniqueTestTopics(tokenWithBearer)
                    topicsAdapter.updateData(topics)
                } catch (e: Exception) {
                    showToast("Error loading topics: ${e.message}")
                }
            }
        } else {
            showToast("Authentication error" + accessToken)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}