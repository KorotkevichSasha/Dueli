package com.example.duelingo.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.R
import com.example.duelingo.activity.auth.LoginActivity
import com.example.duelingo.databinding.ActivityVocabularyBinding
import com.example.duelingo.databinding.DialogAddWordBinding
import com.example.duelingo.dto.request.AddWordRequest
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.WordService
import com.example.duelingo.storage.TokenManager
import com.example.duelingo.utils.UserMessage
import com.example.duelingo.utils.KeyboardInsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VocabularyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVocabularyBinding
    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVocabularyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        tokenManager = TokenManager(this)

        binding.addWordLayout.setOnClickListener {
            showAddWordDialog()
        }

        binding.reviewWordsLayout.setOnClickListener {
            startActivity(Intent(this, WordCardActivity::class.java))
        }

        binding.allWordsLayout.setOnClickListener {
            startActivity(Intent(this, WordListActivity::class.java))
        }

    }

    override fun onResume() {
        super.onResume()
        loadWordCount()
    }

    private fun createAuthenticatedService(): WordService {
        if (!tokenManager.isLoggedIn()) throw IllegalStateException("No token available")
        return ApiClient.wordService
    }

    private fun loadWordCount() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                val words = service.getDueWords()

                withContext(Dispatchers.Main) {
                    val count = words.size
                    binding.tvWordCount.text = if (count == 0) {
                        getString(R.string.words_to_review_zero)
                    } else {
                        "$count ${getCountString(count)}"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("VocabularyActivity", "Error loading words", e)
                    val message = if (e is IllegalStateException) {
                        getString(R.string.session_expired)
                    } else {
                        UserMessage.from(this@VocabularyActivity, e)
                    }
                    Toast.makeText(this@VocabularyActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addWordToBackend(term: String, translation: String, onComplete: (Boolean) -> Unit = {}) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                service.addWord(AddWordRequest(term, translation))

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@VocabularyActivity,
                        "Слово добавлено", Toast.LENGTH_SHORT).show()
                    loadWordCount()
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val message = if (e is IllegalStateException) {
                        getString(R.string.session_expired)
                    } else {
                        UserMessage.from(this@VocabularyActivity, e)
                    }
                    Toast.makeText(this@VocabularyActivity, message, Toast.LENGTH_LONG).show()
                    onComplete(false)
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
        val dialogBinding = DialogAddWordBinding.inflate(layoutInflater)
        val dialog = Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            setOnShowListener {
                findViewById<android.view.View>(android.R.id.content)?.let(KeyboardInsets::apply)
                val width = (resources.displayMetrics.widthPixels - 32 * resources.displayMetrics.density)
                    .toInt().coerceAtMost((520 * resources.displayMetrics.density).toInt())
                window?.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT)
            }
        }
        dialogBinding.closeButton.setOnClickListener { dialog.dismiss() }
        dialogBinding.editTranslation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                dialogBinding.addButton.performClick()
                true
            } else false
        }
        dialogBinding.addButton.setOnClickListener {
            val term = dialogBinding.editTerm.text?.toString()?.trim().orEmpty()
            val translation = dialogBinding.editTranslation.text?.toString()?.trim().orEmpty()
            dialogBinding.termLayout.error = if (term.isBlank()) getString(R.string.required_field) else null
            dialogBinding.translationLayout.error = if (translation.isBlank()) getString(R.string.required_field) else null
            if (term.isBlank() || translation.isBlank()) return@setOnClickListener

            dialogBinding.addButton.isEnabled = false
            dialogBinding.addButton.text = getString(R.string.adding_word)
            addWordToBackend(term, translation) { success ->
                if (success) dialog.dismiss() else {
                    dialogBinding.addButton.isEnabled = true
                    dialogBinding.addButton.text = getString(R.string.add_word_action)
                }
            }
        }
        dialog.show()
    }
}
