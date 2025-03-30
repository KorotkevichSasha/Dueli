package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.databinding.ActivityVocabularyBinding
import com.example.duelingo.dto.request.AddWordRequest
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.WordService
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VocabularyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVocabularyBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVocabularyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)

        binding.addWordLayout.setOnClickListener {
            showAddWordDialog()
        }

        binding.reviewWordsLayout.setOnClickListener {
            startActivity(Intent(this, WordCardActivity::class.java))
        }

        loadWordCount()
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

    private fun loadWordCount() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                val words = service.getDueWords()

                withContext(Dispatchers.Main) {
                    val count = words.size
                    binding.tvWordCount.text = "$count ${getCountString(count)}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VocabularyActivity", "Error loading words", e)
                    when {
                        e is IllegalStateException -> {
                            Toast.makeText(this@VocabularyActivity,
                                "Требуется авторизация", Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@VocabularyActivity,
                                "Сессия истекла, войдите снова", Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@VocabularyActivity,
                            "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun addWordToBackend(term: String, translation: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                service.addWord(AddWordRequest(term, translation))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VocabularyActivity,
                        "Слово добавлено", Toast.LENGTH_SHORT).show()
                    loadWordCount()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when {
                        e is IllegalStateException -> {
                            Toast.makeText(this@VocabularyActivity,
                                "Требуется авторизация", Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@VocabularyActivity,
                                "Сессия истекла, войдите снова", Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("409") == true -> {
                            Toast.makeText(this@VocabularyActivity,
                                "Это слово уже существует", Toast.LENGTH_LONG).show()
                        }
                        else -> Toast.makeText(this@VocabularyActivity,
                            "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    private fun getCountString(count: Int): String {
        return when {
            count % 10 == 1 && count % 100 != 11 -> "слово для повторения"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "слова для повторения"
            else -> "слов для повторения"
        }
    }
    private fun showAddWordDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_word, null)
        val editTerm = dialogView.findViewById<EditText>(R.id.editTerm)
        val editTranslation = dialogView.findViewById<EditText>(R.id.editTranslation)

        AlertDialog.Builder(this)
            .setTitle("Добавить слово")
            .setView(dialogView)
            .setPositiveButton("Добавить") { _, _ ->
                val term = editTerm.text.toString().trim()
                val translation = editTranslation.text.toString().trim()
                if (term.isNotEmpty() && translation.isNotEmpty()) {
                    addWordToBackend(term, translation)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}