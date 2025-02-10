package com.example.duelingo.activity
import QuestionsAdapter
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.duelingo.databinding.ActivityTestDetailsBinding
import com.example.duelingo.dto.response.TestDetailedResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.launch

class TestDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTestDetailsBinding
    private lateinit var questionsAdapter: QuestionsAdapter
    private var testDetails: TestDetailedResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadTestDetails()
    }

    private fun setupRecyclerView() {
        questionsAdapter = QuestionsAdapter(emptyList())
        binding.rvQuestions.apply {
            layoutManager = LinearLayoutManager(this@TestDetailsActivity)
            adapter = questionsAdapter
        }
    }

    private fun loadTestDetails() {
        val testId = intent.getStringExtra("testId") ?: run {
            showToast("Test ID not found")
            finish()
            return
        }

        val token = TokenManager(this).getAccessToken() ?: run {
            showToast("Authentication required")
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Загружаем тест по его ID
                val test = ApiClient.testService.getTestById("Bearer $token", testId)
                testDetails = test
                updateTestInfo(test)

                // Обновляем адаптер с вопросами из теста
                questionsAdapter.updateData(test.questions)
            } catch (e: Exception) {
                showToast("Error loading test: ${e.message}")
            }
        }
    }

    private fun updateTestInfo(test: TestDetailedResponse) {
        binding.tvTestInfo.text = "Тема: ${test.topic}\nСложность: ${test.difficulty}"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}