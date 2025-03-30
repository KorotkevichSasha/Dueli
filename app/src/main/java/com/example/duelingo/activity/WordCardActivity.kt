package com.example.duelingo.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.databinding.ActivityWordCardBinding
import com.example.duelingo.dto.response.WordProgressResponse
import com.example.duelingo.network.WordService
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RequiresApi(Build.VERSION_CODES.O)
class WordCardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWordCardBinding
    private lateinit var tokenManager: TokenManager
    private val words = mutableListOf<WordProgressResponse>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordCardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        loadWords()
        setupCard()
        setupButtons()
    }

    private fun createAuthenticatedService(): WordService {
        val token = tokenManager.getAccessToken() ?: throw IllegalStateException("No token available")

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(AppConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WordService::class.java)
    }

    private fun loadWords() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                val loadedWords = service.getDueWords()

                withContext(Dispatchers.Main) {
                    words.addAll(loadedWords)
                    if (words.isNotEmpty()) {
                        showCurrentWord()
                    } else {
                        Toast.makeText(this@WordCardActivity,
                            "Нет слов для повторения", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when {
                        e is IllegalStateException -> {
                            Toast.makeText(this@WordCardActivity,
                                "Требуется авторизация", Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@WordCardActivity,
                                "Сессия истекла, войдите снова", Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@WordCardActivity,
                            "Ошибка загрузки слов", Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }
        }
    }

    private fun processReview(quality: Int) {
        if (currentIndex >= words.size) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                service.reviewWord(words[currentIndex].wordId, quality)

                currentIndex++

                withContext(Dispatchers.Main) {
                    if (currentIndex < words.size) {
                        showCurrentWord()
                    } else {
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when {
                        e is IllegalStateException -> {
                            Toast.makeText(this@WordCardActivity,
                                "Требуется авторизация", Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@WordCardActivity,
                                "Сессия истекла, войдите снова", Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@WordCardActivity,
                            "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupCard() {
        binding.cardContainer.setOnClickListener {
            flipCard()
        }
    }
    private fun flipCard() {
        val rotationY = if (binding.cardFront.visibility == View.VISIBLE) 180f else 0f
        binding.cardFront.animate()
            .rotationY(rotationY)
            .withEndAction {
                binding.cardFront.visibility = if (rotationY == 0f) View.VISIBLE else View.GONE
                binding.cardBack.visibility = if (rotationY == 0f) View.GONE else View.VISIBLE
            }
            .start()
    }
    private fun setupButtons() {
        binding.btnAgain.setOnClickListener { processReview(0) }
        binding.btnHard.setOnClickListener { processReview(2) }
        binding.btnGood.setOnClickListener { processReview(3) }
        binding.btnEasy.setOnClickListener { processReview(5) }
    }
    private fun showCurrentWord() {
        binding.cardFrontText.text = words[currentIndex].term
        binding.cardBackText.text = words[currentIndex].translation
        binding.cardFront.visibility = View.VISIBLE
        binding.cardBack.visibility = View.GONE
    }
}