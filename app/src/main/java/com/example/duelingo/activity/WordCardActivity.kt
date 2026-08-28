package com.example.duelingo.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.example.duelingo.utils.AppToast as Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.duelingo.databinding.ActivityWordCardBinding
import com.example.duelingo.dto.response.WordProgressResponse
import com.example.duelingo.network.ApiClient
import com.example.duelingo.network.WordService
import com.example.duelingo.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@RequiresApi(Build.VERSION_CODES.O)
class WordCardActivity : AppCompatActivity() {
    companion object {
        private const val STATE_CURRENT_INDEX = "word_current_index"
        private const val STATE_IS_FRONT = "word_is_front"
    }
    private lateinit var binding: ActivityWordCardBinding
    private lateinit var tokenManager: TokenManager
    private val words = mutableListOf<WordProgressResponse>()
    private var currentIndex = 0
    private var isFront = true;
    private var isAnimating = false;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWordCardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.backButton.setOnClickListener { finish() }

        tokenManager = TokenManager(this)
        currentIndex = savedInstanceState?.getInt(STATE_CURRENT_INDEX, 0) ?: 0
        isFront = savedInstanceState?.getBoolean(STATE_IS_FRONT, true) ?: true
        loadWords()
        setupCard()
        setupButtons()
    }

    private fun createAuthenticatedService(): WordService {
        if (!tokenManager.isLoggedIn()) throw IllegalStateException("No token available")
        return ApiClient.wordService
    }

    private fun loadWords() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val service = createAuthenticatedService()
                val loadedWords = service.getDueWords()

                withContext(Dispatchers.Main) {
                    words.addAll(loadedWords)
                    if (words.isNotEmpty()) {
                        currentIndex = currentIndex.coerceIn(0, words.lastIndex)
                        showCurrentWord()
                    } else {
                        Toast.makeText(this@WordCardActivity,
                            getString(com.example.duelingo.R.string.words_to_review_zero), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    when {
                        e is IllegalStateException -> {
                            Toast.makeText(this@WordCardActivity,
                                getString(com.example.duelingo.R.string.session_expired), Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@WordCardActivity,
                                getString(com.example.duelingo.R.string.session_expired), Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@WordCardActivity,
                            getString(com.example.duelingo.R.string.words_loading_error), Toast.LENGTH_SHORT).show()
                    }
                    finish()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_CURRENT_INDEX, currentIndex)
        outState.putBoolean(STATE_IS_FRONT, isFront)
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
                                getString(com.example.duelingo.R.string.session_expired), Toast.LENGTH_SHORT).show()
                        }
                        e.message?.contains("401") == true -> {
                            tokenManager.clearTokens()
                            Toast.makeText(this@WordCardActivity,
                                getString(com.example.duelingo.R.string.session_expired), Toast.LENGTH_SHORT).show()
                        }
                        else -> Toast.makeText(this@WordCardActivity,
                            getString(com.example.duelingo.R.string.word_review_save_error), Toast.LENGTH_SHORT).show()
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
        if (isAnimating) return
        isAnimating = true

        val currentWord = words[currentIndex]
        val outCard = if (isFront) binding.cardFront else binding.cardBack
        val inCard = if (isFront) binding.cardBack else binding.cardFront

        // Устанавливаем текст перед анимацией
        if (isFront) {
            binding.cardBackText.text = currentWord.translation
        } else {
            binding.cardFrontText.text = currentWord.term
        }

        // Настройка "камеры" для 3D-эффекта
        outCard.cameraDistance = 25000f
        inCard.cameraDistance = 25000f

        inCard.rotationY = if (isFront) 90f else -90f
        inCard.visibility = View.VISIBLE

        val outAnim = ObjectAnimator.ofFloat(outCard, "rotationY", 0f, if (isFront) -90f else 90f).apply {
            duration = 200
            interpolator = AccelerateInterpolator()
        }

        val inAnim = ObjectAnimator.ofFloat(inCard, "rotationY", if (isFront) 90f else -90f, 0f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            startDelay = 100
        }

        outAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                outCard.visibility = View.INVISIBLE
            }
        })

        inAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isFront = !isFront
                isAnimating = false
            }
        })

        AnimatorSet().apply {
            playTogether(outAnim, inAnim)
            start()
        }
    }
    private fun setupButtons() {
        binding.btnAgain.setOnClickListener { processReview(0) }
        binding.btnHard.setOnClickListener { processReview(2) }
        binding.btnGood.setOnClickListener { processReview(3) }
        binding.btnEasy.setOnClickListener { processReview(5) }
    }
    private fun showCurrentWord() {
        val currentWord = words[currentIndex]
        binding.nextReviewDate.text = getString(
            com.example.duelingo.R.string.word_card_progress,
            currentIndex + 1,
            words.size
        )
        binding.cardFrontText.text = currentWord.term
        binding.cardBackText.text = currentWord.translation

        if (isFront) {
            binding.cardFront.visibility = View.VISIBLE
            binding.cardBack.visibility = View.INVISIBLE
            binding.cardFront.rotationY = 0f
            binding.cardBack.rotationY = 90f
        } else {
            binding.cardFront.visibility = View.INVISIBLE
            binding.cardBack.visibility = View.VISIBLE
            binding.cardFront.rotationY = -90f
            binding.cardBack.rotationY = 0f
        }
    }
}
